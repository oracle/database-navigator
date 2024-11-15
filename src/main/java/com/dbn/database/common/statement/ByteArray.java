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

package com.dbn.database.common.statement;

import com.dbn.common.exception.Exceptions;
import lombok.Getter;

import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;

@Getter
public class ByteArray implements CallableStatementOutput{
    private byte[] value;

    @Override
    public void registerParameters(CallableStatement statement) throws SQLException {
        statement.registerOutParameter(1, Types.BLOB);
    }

    @Override
    public void read(CallableStatement statement) throws SQLException {
        Blob blob = statement.getBlob(1);
        try {
            value = blob.getBinaryStream().readAllBytes();
        } catch (Exception e) {
            throw Exceptions.toSqlException(e);
        }
    }
}
