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

package com.dbn.connection.info;

import com.dbn.common.util.Unsafe;
import com.dbn.connection.DatabaseType;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static com.dbn.common.util.Strings.toLowerCase;

@Value
public class ConnectionInfo {
    private final DatabaseType databaseType;
    private final String productName;
    private final String productVersion;
    private final String driverName;
    private final String driverVersion;
    private final String driverJdbcType;
    private final String url;
    private final String userName;

    public ConnectionInfo(DatabaseMetaData metaData) throws SQLException {
        productName = metaData.getDatabaseProductName();
        productVersion = resolveProductVersion(metaData);
        driverName = Unsafe.silent("UNKNOWN", metaData, md -> md.getDriverName());
        driverVersion = Unsafe.silent("UNKNOWN", metaData, md -> md.getDriverVersion());
        url = metaData.getURL();
        userName = metaData.getUserName();
        driverJdbcType = resolveDriverType(metaData);
        databaseType = DatabaseType.resolve(toLowerCase(productName));
    }

    @NotNull
    private static String resolveDriverType(DatabaseMetaData metaData) throws SQLException {
        int majorVersion = Unsafe.silent(0, metaData, md -> md.getJDBCMajorVersion());
        int minorVersion = Unsafe.silent(0, metaData, md -> md.getJDBCMinorVersion());
        return majorVersion + (minorVersion > 0 ? "." + minorVersion : "");
    }

    @NotNull
    private static String resolveProductVersion(DatabaseMetaData metaData) throws SQLException {
        String productVersion = Unsafe.silent("UNKNOWN", metaData, md -> md.getDatabaseProductVersion());
        int index = productVersion.indexOf('\n');
        productVersion = index > -1 ? productVersion.substring(0, index) : productVersion;
        return productVersion.trim();
    }

    public String toString() {
        return  "Product name:\t" + productName + '\n' +
                "Product version:\t" + productVersion + '\n' +
                "Driver name:\t\t" + driverName + '\n' +
                "Driver version:\t" + driverVersion + '\n'+
                "JDBC Type:\t\t" + driverJdbcType + '\n' +
                "URL:\t\t" + url + '\n' +
                "User name:\t\t" + userName;
    }
}
