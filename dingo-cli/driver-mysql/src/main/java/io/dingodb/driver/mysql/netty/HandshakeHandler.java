/*
 * Copyright 2021 DataCanvas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.dingodb.driver.mysql.netty;

import io.dingodb.calcite.DingoRootSchema;
import io.dingodb.common.auth.DingoRole;
import io.dingodb.common.environment.ExecutionEnvironment;
import io.dingodb.common.mysql.MysqlByteUtil;
import io.dingodb.common.mysql.MysqlMessage;
import io.dingodb.common.mysql.MysqlServer;
import io.dingodb.common.mysql.Versions;
import io.dingodb.common.mysql.constant.ErrorCode;
import io.dingodb.common.mysql.constant.ServerConstant;
import io.dingodb.common.privilege.UserDefinition;
import io.dingodb.driver.DingoConnection;
import io.dingodb.driver.mysql.MysqlConnection;
import io.dingodb.driver.mysql.command.MysqlResponseHandler;
import io.dingodb.driver.mysql.facotry.SecureChatSslContextFactory;
import io.dingodb.driver.mysql.packet.AuthPacket;
import io.dingodb.driver.mysql.packet.HandshakePacket;
import io.dingodb.driver.mysql.packet.OKPacket;
import io.dingodb.verify.service.UserService;
import io.dingodb.verify.service.UserServiceProvider;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.ssl.SslHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.commons.lang3.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;
import javax.net.ssl.SSLEngine;

import static io.dingodb.common.mysql.Versions.PROTOCOL_VERSION;
import static io.dingodb.common.mysql.constant.ServerStatus.SERVER_STATUS_AUTOCOMMIT;

@ChannelHandler.Sharable
@Slf4j
public class HandshakeHandler extends SimpleChannelInboundHandler<ByteBuf> {
    public MysqlConnection mysqlConnection;

    private byte[] fullSeed;

    public UserService userService;

    public HandshakeHandler(MysqlConnection mysqlConnection) {
        this.mysqlConnection = mysqlConnection;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        HandshakePacket handshakePacket = createHandShakePacket();
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
        handshakePacket.write(buf);
        ctx.writeAndFlush(buf);
        userService = UserServiceProvider.getRoot();
        int seedLength = handshakePacket.seed.length + handshakePacket.seed2.length;
        fullSeed = new byte[seedLength];
        System.arraycopy(handshakePacket.seed, 0, fullSeed, 0, handshakePacket.seed.length);
        System.arraycopy(handshakePacket.seed2, 0, fullSeed,
            handshakePacket.seed.length, handshakePacket.seed2.length);
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        boolean isSSL = false;
        if (!mysqlConnection.authed) {
            AuthPacket authPacket = new AuthPacket();
            if (msg.isReadable()) {
                if (msg.readableBytes() < 5) {
                    return;
                }
                msg.markReaderIndex();
                byte[] headLength = new byte[3];
                msg.readBytes(headLength);
                byte reqPacketId = msg.readByte();
                authPacket.packetId = reqPacketId;
                MysqlMessage mysqlMessage = new MysqlMessage(headLength);
                int contentLength = mysqlMessage.readUB3();
                if (!msg.isReadable(contentLength)) {
                    msg.resetReaderIndex();
                    return;
                }
                byte[] content = new byte[contentLength];
                msg.readBytes(content);
                authPacket.read(content);

                authPacket.packetLength = contentLength;

                isSSL = authPacket.isSSL;
                if (!authPacket.isSSL) {
                    byte[] password = authPacket.password;
                    String user = authPacket.user;

                    String ip = ctx.channel().remoteAddress().toString()
                        .replace("/", "").split(":")[0];
                    if (log.isDebugEnabled()) {
                        log.debug("client ip:" + ip);
                    }
                    UserDefinition userDefinition = userService.getUserDefinition(user, ip);
                    boolean isUserExists = true;
                    String dbPwd = "";
                    if (userDefinition != null) {
                        dbPwd = userDefinition.getPassword();
                    } else {
                        isUserExists = false;
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("real password:" + dbPwd + ", login user:" + user + ", pwd:" + new String(password));
                    }
                    ByteBuf buffer = ByteBufAllocator.DEFAULT.buffer();
                    AtomicLong packetId = new AtomicLong(authPacket.packetId);
                    //mysql protocol packet auto increment based by 0;
                    packetId.incrementAndGet();
                    if (isUserExists && validator(dbPwd, fullSeed, password)) {
                        OKPacket okPacket = new OKPacket();
                        okPacket.capabilities = MysqlServer.getServerCapabilities();
                        okPacket.affectedRows = 0;
                        okPacket.packetId = (byte) packetId.get();
                        okPacket.serverStatus = SERVER_STATUS_AUTOCOMMIT;
                        okPacket.insertId = 0;
                        okPacket.message = "connect success".getBytes();
                        okPacket.write(buffer);
                        ctx.writeAndFlush(buffer);
                        mysqlConnection.authed = true;
                        mysqlConnection.authPacket = authPacket;
                        DingoConnection dingoConnection = (DingoConnection) getLocalConnection();
                        mysqlConnection.setConnection(dingoConnection);
                        MysqlNettyServer.connections.put(dingoConnection.id, mysqlConnection);
                        if (StringUtils.isNotBlank(authPacket.database)) {
                            String usedSchema = authPacket.database.toUpperCase();
                            CalciteSchema schema = dingoConnection.getContext().getRootSchema()
                                .getSubSchema(usedSchema, true);
                            dingoConnection.getContext().setUsedSchema(schema);
                        }
                    } else {
                        ErrorCode.ER_ACCESS_DENIED_ERROR.message =
                            String.format(ErrorCode.ER_ACCESS_DENIED_ERROR.message, user, ip, "YES");
                        MysqlResponseHandler.responseError(packetId,
                            mysqlConnection.channel, ErrorCode.ER_ACCESS_DENIED_ERROR);
                        if (mysqlConnection.channel.isActive()) {
                            mysqlConnection.channel.close();
                        }
                        return;
                    }
                }
            }
        }
        if (isSSL) {
            javax.net.ssl.SSLContext sslCtx = SecureChatSslContextFactory.getServerContext();
            SSLEngine sslEngine = sslCtx.createSSLEngine();
            sslEngine.setUseClientMode(false);
            SslHandler sslHandler = new SslHandler(sslEngine);
            ctx.channel().pipeline().addAfter("handshake", "tls", sslHandler);
            ctx.channel().pipeline().addAfter("tls", "handshake1", this);
            if (msg.isReadable()) {
                ctx.fireChannelRead(msg.retain());
            }
            ctx.channel().pipeline().remove(this);

            if (log.isDebugEnabled()) {
                log.debug("SSLHandler add complete");
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("handshake handler removed");
            }
            ctx.channel().pipeline().remove(this);
        }
    }

    public java.sql.Connection getLocalConnection() {
        try {
            Class.forName("io.dingodb.driver.DingoDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        Properties properties = new Properties();
        properties.setProperty("defaultSchema", DingoRootSchema.DEFAULT_SCHEMA_NAME);
        TimeZone timeZone = TimeZone.getDefault();
        properties.setProperty("timeZone", timeZone.getID());
        properties.setProperty("user", "root");
        properties.setProperty("password", "123123");
        ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        env.setRole(DingoRole.SQLLINE);
        env.putAll(properties);
        java.sql.Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:dingo:", properties);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return connection;
    }

    public static boolean validator(String dbPwd, byte[] seed, byte[] clientPwd) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
        byte[] pwd = MysqlByteUtil.hexStringToByteArray(dbPwd);

        md.update(seed);
        md.update(pwd);
        byte[] toBeXord = md.digest();

        int numToXor = toBeXord.length;

        if (clientPwd.length == 0) {
            if (dbPwd.length() == 0) {
                return true;
            } else {
                return false;
            }
        }
        for (int i = 0; i < numToXor; i++) {
            toBeXord[i] = (byte) (toBeXord[i] ^ clientPwd[i]);
        }
        md.reset();

        byte[] password = md.digest(toBeXord);
        for (int i = 0; i < password.length; i++) {
            if (password[i] != pwd[i]) {
                return false;
            }
        }
        return true;
    }

    private HandshakePacket createHandShakePacket() {
        HandshakePacket handshakePacket = new HandshakePacket();
        handshakePacket.protocolVersion = PROTOCOL_VERSION;
        handshakePacket.serverVersion = Versions.SERVER_VERSION;
        handshakePacket.threadId = 15;
        handshakePacket.seed = createRandomString(8).getBytes();

        handshakePacket.serverCapabilities = MysqlServer.getServerCapabilities();
        handshakePacket.serverCharsetIndex = 0x08;
        handshakePacket.serverStatus = SERVER_STATUS_AUTOCOMMIT;
        handshakePacket.extendedServer = (short) 0xc1ff;
        handshakePacket.authPluginLength = (byte) "mysql_native_password".length();

        handshakePacket.unused = ServerConstant.unused;
        handshakePacket.seed2 = createRandomString(12).getBytes();
        handshakePacket.authPlugin = "mysql_native_password".getBytes();
        AtomicLong packetId = new AtomicLong(0);
        handshakePacket.packetId = (byte) packetId.getAndIncrement();

        return handshakePacket;
    }

    private static Random random = new Random();

    private static char[] ch = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G',
        'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b',
        'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w',
        'x', 'y', 'z', '0', '1'};

    public static synchronized String createRandomString(int length) {
        if (length > 0) {
            int index = 0;
            char[] temp = new char[length];
            int num = random.nextInt();
            for (int i = 0; i < length % 5; i++) {
                temp[index++] = ch[num & 63];
                num >>= 6;
            }
            for (int i = 0; i < length / 5; i++) {
                num = random.nextInt();
                for (int j = 0; j < 5; j++) {
                    temp[index++] = ch[num & 63];
                    num >>= 6;
                }
            }
            return new String(temp, 0, length);
        } else if (length == 0) {
            return "";
        } else {
            throw new IllegalArgumentException();
        }
    }
}