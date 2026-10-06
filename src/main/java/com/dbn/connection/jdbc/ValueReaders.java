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

package com.dbn.connection.jdbc;

import com.dbn.common.util.Streams;
import lombok.experimental.UtilityClass;

import java.io.InputStream;
import java.io.Reader;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.dbn.common.util.Commons.fallback;

@UtilityClass
public final class ValueReaders {

    public static Reader getReader(CallableStatement statement, int parameterIndex) throws SQLException {
        return fallback(
                () -> statement.getCharacterStream(parameterIndex),
                () -> statement.getObject(parameterIndex, Reader.class),
                () -> Streams.reader(statement.getObject(parameterIndex, InputStream.class)));
    }

    public static Reader getReader(ResultSet resultSet, int columnIndex) throws SQLException {
        return fallback(
                () -> resultSet.getCharacterStream(columnIndex),
                () -> resultSet.getObject(columnIndex, Reader.class),
                () -> Streams.reader(resultSet.getObject(columnIndex, InputStream.class)));
    }

}
