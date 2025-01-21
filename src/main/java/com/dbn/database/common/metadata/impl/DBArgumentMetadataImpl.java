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

package com.dbn.database.common.metadata.impl;

import com.dbn.database.common.metadata.DBObjectMetadataBase;
import com.dbn.database.common.metadata.def.DBArgumentMetadata;
import com.dbn.database.common.metadata.def.DBDataTypeMetadata;
import lombok.Getter;

import java.sql.ResultSet;
import java.sql.SQLException;

@Getter
public class DBArgumentMetadataImpl extends DBObjectMetadataBase implements DBArgumentMetadata {
    private final DBDataTypeMetadata dataType;

    public DBArgumentMetadataImpl(ResultSet resultSet) {
        super(resultSet);
        dataType = new DBDataTypeMetadataImpl(resultSet);
    }

    @Override
    public String getArgumentName() throws SQLException {
        return getString("ARGUMENT_NAME");
    }

    @Override
    public String getProgramName() throws SQLException {
        return getString("PROGRAM_NAME");
    }

    @Override
    public String getMethodName() throws SQLException {
        return getString("METHOD_NAME");
    }

    @Override
    public String getMethodType() throws SQLException {
        return getString("METHOD_TYPE");
    }

    @Override
    public String getInOut() throws SQLException {
        return getString("IN_OUT");
    }

    @Override
    public short getOverload() throws SQLException {
        return resultSet.getShort("OVERLOAD");
    }

    @Override
    public short getPosition() throws SQLException {
        return resultSet.getShort("POSITION");
    }

    @Override
    public short getSequence() throws SQLException {
        return resultSet.getShort("SEQUENCE");
    }
}
