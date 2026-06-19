/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.common.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class JdbcUrlsTest {

    @Test
    public void testRedactNullUrl() {
        assertNull(JdbcUrls.redactSensitiveParameters(null));
    }

    @Test
    public void testRedactUriUserInfoCredentialsMySql() {
        String url = "jdbc:mysql://alice:Secret123@localhost:3306/demo";
        String expected = "jdbc:mysql://redacted_user:redacted_password@localhost:3306/demo";
        String redacted = JdbcUrls.redactSensitiveParameters(url);

        assertEquals(expected, redacted);
        assertFalse(redacted.contains("Secret123"));
    }

    @Test
    public void testRedactOracleInlineCredentials() {
        String url = "jdbc:oracle:thin:scott/tiger@localhost:1521/orclpdb";
        String expected = "jdbc:oracle:thin:redacted_user/redacted_password@localhost:1521/orclpdb";
        String redacted = JdbcUrls.redactSensitiveParameters(url);

        assertEquals(expected, redacted);
        assertFalse(redacted.contains("tiger"));
    }

    @Test
    public void testRedactSensitiveQueryParameters() {
        String url = "jdbc:postgresql://localhost/db?user=alice&password=Secret123&sslmode=require";
        String expected = "jdbc:postgresql://localhost/db?user=redacted_user&password=redacted_password&sslmode=require";
        String redacted = JdbcUrls.redactSensitiveParameters(url);

        assertEquals(expected, redacted);
        assertFalse(redacted.contains("Secret123"));
    }

    @Test
    public void testRedactSensitiveSemicolonParameters() {
        String url = "jdbc:sqlserver://host:1433;databaseName=mydb;user=alice;pwd=Secret123;encrypt=true";
        String expected = "jdbc:sqlserver://host:1433;databaseName=mydb;user=redacted_user;pwd=redacted_pwd;encrypt=true";
        String redacted = JdbcUrls.redactSensitiveParameters(url);

        assertEquals(expected, redacted);
        assertFalse(redacted.contains("Secret123"));
    }

    @Test
    public void testRedactSensitiveSemicolonParametersWithBracedValue() {
        String url = "jdbc:sqlserver://host:1433;user={myUser};password={myPass;word};encrypt=true";
        String expected = "jdbc:sqlserver://host:1433;user=redacted_user;password=redacted_password;encrypt=true";
        String redacted = JdbcUrls.redactSensitiveParameters(url);

        assertEquals(expected, redacted);
        assertFalse(redacted.contains("{myPass;word}"));
        assertFalse(redacted.contains("myPass"));
    }

    @Test
    public void testRedactSqlServerPasswordContainingAtSign() {
        String url = "jdbc:sqlserver://host:1433;databaseName=mydb;user=alice;password=Secret@123;encrypt=true";
        String expected = "jdbc:sqlserver://host:1433;databaseName=mydb;user=redacted_user;password=redacted_password;encrypt=true";
        String redacted = JdbcUrls.redactSensitiveParameters(url);

        assertEquals(expected, redacted);
        assertFalse(redacted.contains("Secret"));
        assertFalse(redacted.contains("123"));
    }

    @Test
    public void testRedactSensitiveMySqlPackedAddressParameters() {
        String url = "jdbc:mysql://address=(host=db1.example.com,port=3306,user=alice,password=Secret123)/sales";
        String expected = "jdbc:mysql://address=(host=db1.example.com,port=3306,user=redacted_user,password=redacted_password)/sales";
        String redacted = JdbcUrls.redactSensitiveParameters(url);

        assertEquals(expected, redacted);
        assertFalse(redacted.contains("Secret123"));
    }

    @Test
    public void testRedactMySqlPackedAddressPasswordContainingAtSign() {
        String url = "jdbc:mysql://address=(host=db1.example.com,port=3306,user=alice,password=Secret@123)/sales";
        String expected = "jdbc:mysql://address=(host=db1.example.com,port=3306,user=redacted_user,password=redacted_password)/sales";
        String redacted = JdbcUrls.redactSensitiveParameters(url);

        assertEquals(expected, redacted);
        assertFalse(redacted.contains("Secret"));
        assertFalse(redacted.contains("123"));
    }

    @Test
    public void testRedactMySqlMultiFactorPasswordAliases() {
        String url = "jdbc:mysql://localhost:3306/sales?password1=FirstSecret&password2=SecondSecret&password3=ThirdSecret";
        String expected = "jdbc:mysql://localhost:3306/sales?password1=redacted_password1&password2=redacted_password2&password3=redacted_password3";
        String redacted = JdbcUrls.redactSensitiveParameters(url);

        assertEquals(expected, redacted);
        assertFalse(redacted.contains("FirstSecret"));
        assertFalse(redacted.contains("SecondSecret"));
        assertFalse(redacted.contains("ThirdSecret"));
    }

    @Test
    public void testRedactSensitiveDescriptorParameters() {
        String url = "jdbc:oracle:thin:@(USER=alice)(PASSWORD=Secret123)(SERVICE_NAME=pdb1)";
        String expected = "jdbc:oracle:thin:@(USER=redacted_user)(PASSWORD=redacted_password)(SERVICE_NAME=pdb1)";
        String redacted = JdbcUrls.redactSensitiveParameters(url);

        assertEquals(expected, redacted);
        assertFalse(redacted.contains("Secret123"));
    }

    @Test
    public void testRedactPlaceholderUsesNormalizedKeyName() {
        String url = "jdbc:oracle:thin:@//host:1521/db?oracle.jdbc.clientSecret=abc&api-key=xyz";
        String expected = "jdbc:oracle:thin:@//host:1521/db?oracle.jdbc.clientSecret=redacted_oracle_jdbc_clientsecret&api-key=redacted_api_key";
        String redacted = JdbcUrls.redactSensitiveParameters(url);

        assertEquals(expected, redacted);
        assertFalse(redacted.contains("abc"));
        assertFalse(redacted.contains("xyz"));
    }

    @Test
    public void testRedactionIsIdempotent() {
        String url = "jdbc:mysql://alice:Secret@localhost:3306/db?user=alice&password=secret";
        String once = JdbcUrls.redactSensitiveParameters(url);
        String twice = JdbcUrls.redactSensitiveParameters(once);

        assertEquals(once, twice);
    }

    @Test
    public void testKeepBenignParametersUntouched() {
        String url = "jdbc:postgresql://localhost/db?sslmode=require&connectTimeout=15";
        assertEquals(url, JdbcUrls.redactSensitiveParameters(url));
    }
}
