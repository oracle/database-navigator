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

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

@NonNls
public class TnsProfilePattern {
    public static TnsProfilePattern INSTANCE = new TnsProfilePattern();

    private final Pattern pattern;

    private TnsProfilePattern() {
        pattern = initPattern();
    }

    @NotNull
    private Pattern initPattern() {
        // TODO support full TNS syntax (https://docs.oracle.com/en/database/oracle/oracle-database/19/netrf/local-naming-parameters-in-tns-ora-file.html#GUID-7F967CE5-5498-427C-9390-4A5C6767ADAA)
        String value = "[A-Z0-9._-]+";
        String any =          keyValue("[_A-Z]+",      value);

        String community1 =   keyValue("COMMUNITY", group("community1", value));
        String protocol1 =    keyValue("PROTOCOL", group("protocol1", value) );
        String host1 =        keyValue("HOST",     group("host1", value));
        String port1 =        keyValue("PORT",     group("port1", value));

        String community2 =   keyValue("COMMUNITY", group("community2", value));
        String protocol2 =    keyValue("PROTOCOL",  group("protocol2", value));
        String host2 =        keyValue("HOST",      group("host2", value));
        String port2 =        keyValue("PORT",      group("port2", value));


        String community3 =   keyValue("COMMUNITY", group("community3", value));
        String protocol3 =    keyValue("PROTOCOL",  group("protocol3", value));
        String host3 =        keyValue("HOST",      group("host3", value));
        String port3 =        keyValue("PORT",      group("port3", value));

        String address1 =     keyValue("ADDRESS",     iteration(block(oneOf(community1, protocol1, host1, port1, any))));
        String address2 =     keyValue("ADDRESS",     iteration(block(oneOf(community2, protocol2, host2, port2, any))));
        String address3 =     keyValue("ADDRESS",     iteration(block(oneOf(community3, protocol3, host3, port3, any))));

        String retryCount =   keyValue("RETRY_COUNT", group("retrycount", value));
        String retryDelay =   keyValue("RETRY_DELAY", group("retrydelay", value));
        String security =     keyValue("SECURITY",    iteration(block(any)));


        String sid =          keyValue("SID",          group("sid", value));
        String server =       keyValue("SERVER",       group("server", value));
        String serviceName =  keyValue("SERVICE_NAME", group("servicename", value) );
        String globalName =   keyValue("GLOBAL_NAME",  group("globalname", value));

        String type =         keyValue("TYPE",         group("failovertype", value));
        String method =       keyValue("METHOD",       group("failovermethod", value));

        String sdu =          keyValue("SDU",           group("sdu", value));
        String failover =     keyValue("FAILOVER",      group("failover", value));
        String failoverMode = keyValue("FAILOVER_MODE", iteration(block(oneOf(type, method, any))));
        String addressList =  keyValue("ADDRESS_LIST",  iteration(block(address3)));
        String connectData =  keyValue("CONNECT_DATA",  iteration(block(oneOf(sid, server, serviceName, globalName, failoverMode, any))));
        String description =  keyValue("DESCRIPTION",   iteration(block(oneOf(sdu, security, retryCount, retryDelay, failover, address2, addressList, connectData))));
        String block =        keyValue(group("schema", value), group("descriptor", block(oneOf(address1, description))));

        return Pattern.compile(block, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    }

    public Pattern get() {
        return pattern;
    }

    @NonNls
    private static String block(String content) {
        return "[(]\\s*" + content + "\\s*[)]";
    }

    @NonNls
    private static String keyValue(@NonNls String key, @NonNls String value) {
        return key + "\\s*=\\s*" + value;
    }

    private static String group(String name, String content) {
        return "(?<" + name + ">" + content + ")";
    }

    private static String oneOf(String ... contents) {
        StringBuilder result = new StringBuilder();
        for (String content : contents) {
            if (result.length() > 0) {
                result.append("|");
            }
            result.append("(").append(content).append(")");
        }

        return "(" + result + ")";
    }
    private static String iteration(String content) {
        return "(" + content + "\\s*)*";
    }
}
