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

package io.dingodb.net;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

//TODO: this should be in meta api to reduce dependencies.
@ToString(of = {"host", "port", "path"})
@EqualsAndHashCode
public class Location {
    @JsonProperty("host")
    @Getter
    private final String host;
    @JsonProperty("port")
    @Getter
    private final int port;
    @JsonProperty("path")
    @Getter
    private final String path;

    @JsonCreator
    public Location(
        @JsonProperty("host") String host,
        @JsonProperty("port") int port,
        @JsonProperty("path") String path
    ) {
        this.host = host;
        this.port = port;
        this.path = path;
    }
}
