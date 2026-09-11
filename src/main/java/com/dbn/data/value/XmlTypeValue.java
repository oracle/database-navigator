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

package com.dbn.data.value;

import com.dbn.common.reflection.ObjectProxies;
import com.dbn.common.reflection.ProxyObject;
import com.dbn.common.util.Safe;
import com.dbn.data.type.GenericDataType;
import com.dbn.database.oracle.jdbc.OracleCallableStatement;
import com.dbn.database.oracle.jdbc.OracleResultSet;
import com.dbn.database.oracle.jdbc.OracleXmlType;
import org.jetbrains.annotations.Nullable;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.dbn.common.util.Commons.fallback;
import static com.dbn.connection.jdbc.DBNResource.unwrap;
import static com.dbn.database.oracle.jdbc.OracleXmlType.createXML;

public class XmlTypeValue extends LargeObjectValue {
    private OracleXmlType xmlType;

    public XmlTypeValue() {
    }

    public XmlTypeValue(CallableStatement callableStatement, int parameterIndex) throws SQLException {
        callableStatement = unwrap(callableStatement);
        OracleCallableStatement oracleCallableStatement = ObjectProxies.create(callableStatement, OracleCallableStatement.class);
        Object opaque = oracleCallableStatement.getOPAQUE(parameterIndex);
        if (opaque == null) return;

        xmlType = createXML(opaque);
    }

    public XmlTypeValue(ResultSet resultSet, int columnIndex) throws SQLException {
        resultSet = unwrap(resultSet);

        OracleResultSet oracleResultSet = ObjectProxies.create(resultSet, OracleResultSet.class);
        Object opaque = oracleResultSet.getOPAQUE(columnIndex);
        if (opaque == null) return;

        xmlType = createXML(opaque);
    }



    @Override
    public GenericDataType getGenericDataType() {
        return GenericDataType.XMLTYPE;
    }

    @Override
    @Nullable
    public String read() throws SQLException {
        return read(0);
    }

    @Nullable
    @Override
    public String export() throws SQLException {
        return read();
    }

    @Override
    @Nullable
    public String read(int maxSize) throws SQLException {
        if (xmlType == null) return null;

        Reader reader = fallback(
                () -> Safe.call(xmlType.getClobVal(), c -> c.getCharacterStream()),
                () -> Safe.call(xmlType.getInputStream(), s -> new InputStreamReader(s, StandardCharsets.UTF_8)),
                () -> Safe.call(xmlType.getStringVal(), StringReader::new));

        return readCharacterStream(reader, maxSize);
    }


    @Override
    public void write(Connection connection, PreparedStatement preparedStatement, int parameterIndex, @Nullable String value) throws SQLException {
        connection = unwrap(connection);
        preparedStatement = unwrap(preparedStatement);

        xmlType = createXML(preparedStatement, connection, value);
        preparedStatement.setObject(parameterIndex, ProxyObject.unwrap(xmlType));
    }

    @Override
    public void write(Connection connection, ResultSet resultSet, int columnIndex, @Nullable String value) throws SQLException {
        connection = unwrap(connection);
        resultSet = unwrap(resultSet);

        xmlType = value == null ? null : createXML(resultSet, connection, value);
        OracleResultSet oracleResultSet = ObjectProxies.create(resultSet, OracleResultSet.class);
        oracleResultSet.updateOracleObject(columnIndex, xmlType);
    }

    @Override
    public void release() {

    }

    @Override
    public long size() throws SQLException {
        return 0;
    }

    @Override
    public String getDisplayValue() {
        return "[XMLTYPE]";
    }
}
