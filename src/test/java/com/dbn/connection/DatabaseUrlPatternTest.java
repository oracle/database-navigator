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

package com.dbn.connection;

import org.jetbrains.annotations.NonNls;
import org.junit.Assert;
import org.junit.Test;

import static com.dbn.connection.DatabaseUrlPattern.MYSQL_DB;
import static com.dbn.connection.DatabaseUrlPattern.ORACLE_LDAP;
import static com.dbn.connection.DatabaseUrlPattern.ORACLE_LDAPS;
import static com.dbn.connection.DatabaseUrlPattern.ORACLE_SERVICE;
import static com.dbn.connection.DatabaseUrlPattern.ORACLE_SID;
import static com.dbn.connection.DatabaseUrlPattern.ORACLE_TNS;
import static com.dbn.connection.DatabaseUrlPattern.POSTGRES_DB;
import static com.dbn.connection.DatabaseUrlPattern.REDSHIFT_DB;
import static com.dbn.connection.DatabaseUrlPattern.SQLITE_FILE;

public class DatabaseUrlPatternTest {

    @Test
    public void testPatterns() {
        test(ORACLE_TNS,
                "jdbc:oracle:thin:@PROFILE_ABC?TNS_ADMIN=C:\\Test\\TNS admin",
                "jdbc:oracle:oci:@PROFILE_ABC?TNS_ADMIN=C:\\Test\\TNS admin",
                "jdbc:oracle:thin:@PROFILE_ABC?TNS_ADMIN=C:/Test/TNS admin.tmp",
                "jdbc:oracle:thin:@PROFILE.ABC?TNS_ADMIN=/Test/TNS admin.tmp");

        test(ORACLE_SID,
                "jdbc:oracle:thin:@host123:1234:XE",
                "jdbc:oracle:oci:@localhost:1234:XE",
                "jdbc:oracle:thin:@host_abc:1234:XE.ch",
                "jdbc:oracle:thin:@192.168.1.1:1234:XE.ch",
                "jdbc:oracle:thin:@host.domain.net:1234:XE");

        test(ORACLE_SERVICE,
                "jdbc:oracle:thin:@//host123:1234/SRV.AB",
                "jdbc:oracle:oci:@//localhost:1234/XE",
                "jdbc:oracle:thin:@//host_abc:1234/XE.ch",
                "jdbc:oracle:thin:@//192.168.1.1:1234/XE.ch",
                "jdbc:oracle:thin:@//host.domain.net:1234/XE");


        test(ORACLE_LDAP,
                "jdbc:oracle:thin:@ldap://host123:1234/SRV.AB",
                "jdbc:oracle:oci:@ldap://localhost:1234/XE",
                "jdbc:oracle:thin:@ldap://host_abc:1234/XE.ch",
                "jdbc:oracle:thin:@ldap://192.168.1.1:1234/XE.ch",
                "jdbc:oracle:thin:@ldap://host.domain.net:1234/XE");


        test(ORACLE_LDAPS,
                "jdbc:oracle:thin:@ldaps://host123:1234/SRV.AB",
                "jdbc:oracle:oci:@ldaps://localhost:1234/XE",
                "jdbc:oracle:thin:@ldaps://host_abc:1234/XE.ch",
                "jdbc:oracle:thin:@ldaps://192.168.1.1:1234/XE.ch",
                "jdbc:oracle:thin:@ldaps://host.domain.net:1234/XE");


        test(MYSQL_DB,
                "jdbc:mysql://host123:1234/mysqldb",
                "jdbc:mysql://localhost:1234/db",
                "jdbc:mysql://host_abc:0000/db1.net",
                "jdbc:mysql://192.168.1.1:123654/mysql.db",
                "jdbc:mysql://host.domain.net:1234/mysqldb1");

        test(POSTGRES_DB,
                "jdbc:postgresql://host123:1234/pgdb",
                "jdbc:postgresql://localhost:1234/db",
                "jdbc:postgresql://host_abc:0000/db1.net",
                "jdbc:postgresql://192.168.1.1:123654/pg.db",
                "jdbc:postgresql://host.domain.net:1234/pgdb1");

        test(REDSHIFT_DB,
                "jdbc:redshift://host123:1234/pgdb",
                "jdbc:redshift://localhost:1234/db",
                "jdbc:redshift://host_abc:0000/db1.net",
                "jdbc:redshift://192.168.1.1:123654/pg.db",
                "jdbc:redshift://host.domain.net:1234/pgdb1");

        test(SQLITE_FILE,
                "jdbc:sqlite:",
                "jdbc:sqlite:C:\\Test\\sqlite.db",
                "jdbc:sqlite:C:\\Test\\sqlite_1.db",
                "jdbc:sqlite:/test/sqlite databases/sqlite.db",
                "jdbc:sqlite:/Test1/dbs/sqlite");

    }

    private static void test(DatabaseUrlPattern pattern, @NonNls String ... urls) {
        for (String url : urls) {
            System.out.println(url);
            Assert.assertTrue(pattern.name() +  ": url " + url + " invalid", pattern.getUrlPattern().matcher(url).matches());
        }

    }
}