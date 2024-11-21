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

package com.dbn.data.value;

import com.dbn.connection.jdbc.DBNResource;
import lombok.SneakyThrows;

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.common.util.Unsafe.silent;

class XmlTypeDelegate {

    private static final Map<ClassLoader, XmlTypeDelegate> instances = new ConcurrentHashMap<>();

    private final Class<?> xmlTypeClass;
    private final Class<?> opaqueClass;
    private final Class<?> datumClass;
    private final Class<?> statementClass;
    private final Class<?> resultSetClass;

    private final Method statementOpaqueMethod;
    private final Method resultSetOpaqueMethod;
    private final Method createXmlFromOpaqueMethod;
    private final Method createXmlFromStringMethod;
    private final Method updateResultSetObjectMethod;
    private final Method stringValueMethod;

    @SneakyThrows
    private XmlTypeDelegate(ClassLoader classLoader) {
        xmlTypeClass = classLoader.loadClass("oracle.xdb.XMLType");
        opaqueClass = classLoader.loadClass("oracle.sql.OPAQUE");
        datumClass = classLoader.loadClass("oracle.sql.Datum");
        statementClass = classLoader.loadClass("oracle.jdbc.OracleCallableStatement");
        resultSetClass = classLoader.loadClass("oracle.jdbc.OracleResultSet");

        statementOpaqueMethod = statementClass.getMethod("getOPAQUE", int.class);
        resultSetOpaqueMethod = resultSetClass.getMethod("getOPAQUE", int.class);
        createXmlFromOpaqueMethod = xmlTypeClass.getMethod("createXML", opaqueClass);
        createXmlFromStringMethod = xmlTypeClass.getMethod("createXML", Connection.class, String.class);
        updateResultSetObjectMethod = resultSetClass.getMethod("updateOracleObject", int.class, datumClass);
        stringValueMethod = xmlTypeClass.getMethod("getStringVal");
    }

    static XmlTypeDelegate get(Object object) {
        if (object instanceof DBNResource) {
            object = ((DBNResource<?>) object).getInner();
        }

        ClassLoader classLoader = object.getClass().getClassLoader();
        return instances.computeIfAbsent(classLoader, cl -> new XmlTypeDelegate(cl));
    }

    Object getOpaque(CallableStatement callableStatement, int parameterIndex) {
        return invoke(statementOpaqueMethod, callableStatement, parameterIndex);
    }

    Object getOpaque(ResultSet resultSet, int columnIndex) {
        return invoke(resultSetOpaqueMethod, resultSet, columnIndex);
    }

    Object createXml(Object opaque) {
        return invoke(createXmlFromOpaqueMethod, null, opaque);
    }

    Object createXml(Connection connection, String value) {
        return invoke(createXmlFromStringMethod, null, connection, value);
    }

    String getStringValue(Object xmlType) {
        return invoke(stringValueMethod, xmlType);
    }

    void updateResultSetObject(ResultSet resultSet, int columnIndex, Object xmlType) {
        invoke(updateResultSetObjectMethod, resultSet, columnIndex, xmlType);
    }

    boolean isXmlType(Object opaque) {
        return xmlTypeClass.isAssignableFrom(opaque.getClass());
    }

    private static <T> T invoke(Method method, Object obj, Object ... args) {
        return cast(silent(null, () -> method.invoke(obj, args)));
    }
}
