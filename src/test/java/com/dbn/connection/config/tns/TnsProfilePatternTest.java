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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TnsProfilePatternTest {

    @Test
    public void get() {
        Pattern pattern = TnsProfilePattern.INSTANCE.get();

        Matcher matcher = pattern.matcher("dcidbn0001_high = (description= (retry_count=20)(retry_delay=3)(address=(protocol=tcps)(port=1522)(host=adb.eu-zurich-1.oraclecloud.com))(connect_data=(service_name=g47875f42217f9e_dcidbn0001_high.adb.oraclecloud.com))(security=(ssl_server_dn_match=yes)))");
        boolean matches = matcher.matches();
        assertTrue(matches);
    }
    @Test
    public void getMultiline() {
        Pattern pattern = TnsProfilePattern.INSTANCE.get();

        String str = "azure_db = (DESCRIPTION=\n" +
                "  (ADDRESS=\n" +
                "    (PROTOCOL=tcps)\n" +
                "    (PORT=1522)\n" +
                "    (HOST=adb.us-phoenix-1.oraclecloud.com))\n" +
                "  (CONNECT_DATA=\n" +
                "    (SERVICE_NAME=zzzzzzzzzzzzzzz_azuredb_medium.adb.oraclecloud.com))\n" +
                "  (SECURITY=\n" +
                "    (SSL_SERVER_DN_MATCH=yes)))";
        Matcher matcher = pattern.matcher(str);
        assertTrue(matcher.matches());
        assertEquals(0, matcher.start());
        assertEquals(str.length(), matcher.end());
    }

    @Test
    public void testMultilineWithLeadingComments() {
        Pattern pattern = TnsProfilePattern.INSTANCE.get();

        String str = "# Parameters configure interactive authentication with Azure. All interaction\n" +
                "# happens in a web browser.\n" +
                "azure_interactive = (DESCRIPTION=\n" +
                "  (ADDRESS=\n" +
                "    (PROTOCOL=tcps)\n" +
                "    (PORT=1522)\n" +
                "    (HOST=adb.us-phoenix-1.oraclecloud.com))\n" +
                "  (CONNECT_DATA=\n" +
                "    (SERVICE_NAME=zzzzzzzzzzzz_azuredb_medium.adb.oraclecloud.com))\n" +
                "  (SECURITY=\n" +
                "    (SSL_SERVER_DN_MATCH=yes)\n" +
                "    (AZURE_DB_APP_ID_URI=https://oracledevelopment.onmicrosoft.com/12345-abcdef-23345)\n" +
                "    (TOKEN_AUTH=azure_interactive)))";
        Matcher matcher = pattern.matcher(str);
        assertTrue(matcher.find());
        System.out.println(str.substring(matcher.start()));
        assertEquals(106, matcher.start());
        assertEquals(str.length(), matcher.end());
    }
}