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

import org.junit.Assert;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TnsProfilePatternTest {

    @Test
    public void get() {
        Pattern pattern = TnsProfilePattern.INSTANCE.get();

        Matcher matcher = pattern.matcher("dcidbn0001_high = (description= (retry_count=20)(retry_delay=3)(address=(protocol=tcps)(port=1522)(host=adb.eu-zurich-1.oraclecloud.com))(connect_data=(service_name=g47875f42217f9e_dcidbn0001_high.adb.oraclecloud.com))(security=(ssl_server_dn_match=yes)))");
        boolean matches = matcher.matches();
        Assert.assertTrue(matches);
    }
}