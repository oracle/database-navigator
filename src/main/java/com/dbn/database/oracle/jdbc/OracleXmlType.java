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

package com.dbn.database.oracle.jdbc;

import com.dbn.common.exception.Exceptions;
import com.dbn.common.reflection.ObjectProxies;
import com.dbn.common.reflection.ProxyObject;
import com.dbn.common.reflection.ProxyObjectInfo;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLException;

import static com.dbn.common.Reflection.invokeMethod;

/**
 * Proxy of {@code oracle.xdb.XMLType}.
 */
@ProxyObjectInfo(delegateClass = "oracle.xdb.XMLType")
public interface OracleXmlType extends ProxyObject {

    Clob getClobVal() throws SQLException;

    InputStream getInputStream() throws SQLException;

    String getStringVal() throws SQLException;


    @Nullable
    static OracleXmlType createXML(Object opaque) throws SQLException {
        if (opaque == null) return null;

        Object xmlType = createXmlType(opaque, opaque);
        return ObjectProxies.create(xmlType, OracleXmlType.class);
    }

    @Nullable
    static OracleXmlType createXML(AutoCloseable resource, Connection connection, @Nullable String value) throws SQLException {
        if (value == null) return null;

        Object xmlType = createXmlType(resource, connection, value);
        return ObjectProxies.create(xmlType, OracleXmlType.class);
    }

    static Object createXmlType(Object source, Object... arguments) throws SQLException {
        try {
            ClassLoader classLoader = source.getClass().getClassLoader();
            Class<?> xmlTypeClass = classLoader.loadClass("oracle.xdb.XMLType");

            return invokeMethod(xmlTypeClass, "createXML", arguments);
        } catch (Throwable e) {
            throw Exceptions.toSqlException(e);
        }
    }
}
