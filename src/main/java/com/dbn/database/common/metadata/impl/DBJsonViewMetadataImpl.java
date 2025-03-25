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

import com.dbn.database.common.metadata.def.DBJsonViewMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DBJsonViewMetadataImpl extends DBViewMetadataImpl implements DBJsonViewMetadata {

    public DBJsonViewMetadataImpl(ResultSet resultSet) {
        super(resultSet);
    }

    @Override
    public boolean isValid() throws SQLException {
        return isYesFlag("IS_VALID");
    }

    @Override
    public boolean isReadonly() throws SQLException {
        return isYesFlag("IS_READONLY");
    }

    @Override
    public String getRootTableOwner() throws SQLException {
        return getString("ROOT_TABLE_OWNER");
    }

    @Override
    public String getRootTableName() throws SQLException {
        return getString("ROOT_TABLE_NAME");
    }

    @Override
    public String getJsonSchema() throws SQLException {
        return getString("JSON_SCHEMA");
    }

    @Override
    public String getJsonColumnName() throws SQLException {
        return getString( "JSON_COLUMN_NAME");
    }
}
