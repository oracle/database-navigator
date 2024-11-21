/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.connection.config.tns;


import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public class TnsProfile implements Comparable<TnsProfile> {
    private final String descriptor;

    private final String profile;
    private final String protocol;
    private final String host;
    private final String port;
    private final String server;
    private final String sid;
    private final String serviceName;
    private final String globalName;
    private final String failover;
    private final String failoverType;
    private final String failoverMethod;
    private boolean selected;

    TnsProfile(
            String descriptor,
            String name,
            String protocol,
            String host,
            String port,
            String server,
            String sid,
            String serviceName,
            String globalName,
            String failover,
            String failoverType,
            String failoverMethod) {
        this.descriptor = descriptor;

        this.profile = name;
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.server = server;
        this.sid = sid;
        this.serviceName = serviceName;
        this.globalName = globalName;
        this.failover = failover;
        this.failoverType = failoverType;
        this.failoverMethod = failoverMethod;
    }

    public String toString() {
        return profile;
    }

    @Override
    public int compareTo(@NotNull TnsProfile o) {
        return profile.compareTo(o.profile);
    }
}
