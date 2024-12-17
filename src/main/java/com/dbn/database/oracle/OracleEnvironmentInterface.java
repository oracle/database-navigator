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

package com.dbn.database.oracle;

import com.dbn.common.util.Strings;
import com.dbn.database.interfaces.DatabaseEnvironmentInterface;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import static com.dbn.common.util.Commons.nvl;

public class OracleEnvironmentInterface implements DatabaseEnvironmentInterface {
    public static final String CLOUD_DATABASE_PATTERN = ".+\\.ade\\..+\\.oraclecloud\\.com";
    public static final List<String> cloudHostnames = Strings.tokenize(nvl(System.getProperty("cloud.hostnames"), ""), ",");


    @Override
    public boolean isCloudDatabase(String hostname) {
        if (Strings.isEmptyOrSpaces(hostname)) return false;
        if (hostname.matches(CLOUD_DATABASE_PATTERN)) return true;
        if (cloudHostnames.contains(hostname)) return true;

        // TODO all false from here, do we need these?
        if (hostname.equals("localhost")) return false;
        if (hostname.equals("127.0.0.1")) return false;
        try {
            if (hostname.equals(InetAddress.getLocalHost().getHostAddress())) return false;
        } catch (UnknownHostException e) {
            return false;
        }

        return false;
    }
}
