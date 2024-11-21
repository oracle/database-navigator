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

import com.dbn.database.common.metadata.def.DBDataTypeMetadata;
import com.dbn.database.common.metadata.def.DBTypeMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class DBTypeMetadataImpl extends DBProgramMetadataImpl implements DBTypeMetadata {
    private final DBDataTypeMetadata dataType;

    public DBTypeMetadataImpl(ResultSet resultSet) {
        super(resultSet);
        dataType = new DBDataTypeMetadataImpl(resultSet);
    }

    public String getTypeName() throws SQLException {
        return getString("TYPE_NAME");
    }

    public String getTypeCode() throws SQLException {
        return getString("TYPECODE");
    }

    public String getSupertypeOwner() throws SQLException {
        return getString("SUPERTYPE_OWNER");
    }

    public String getSupertypeName() throws SQLException {
        return getString("SUPERTYPE_NAME");
    }

    public boolean isCollection() throws SQLException {
        String typeCode = getTypeCode();
        return Objects.equals(typeCode, "COLLECTION");
    }

    @Override
    public String getPackageName() throws SQLException {
        return getString("PACKAGE_NAME");
    }

    @Override
    public DBDataTypeMetadata getDataType() {
        return dataType;
    }
}
