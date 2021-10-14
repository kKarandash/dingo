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

package io.dingodb.cli.handler;

import io.dingodb.server.ServerConfiguration;
import org.apache.helix.manager.zk.ZKHelixAdmin;
import org.apache.helix.model.IdealState;

public class ResourceTagHandler {

    public static void handler(String name, String tag) {
        ServerConfiguration configuration = ServerConfiguration.instance();
        String zkServers = configuration.zkServers();
        String clusterName = configuration.clusterName();

        ZKHelixAdmin helixAdmin = new ZKHelixAdmin.Builder().setZkAddress(zkServers).build();

        IdealState idealState = helixAdmin.getResourceIdealState(clusterName, name);
        idealState.setInstanceGroupTag(tag);
        helixAdmin.setResourceIdealState(clusterName, name, idealState);
    }

}
