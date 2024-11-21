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
import com.dbn.database.common.metadata.def.DBDataTypeMetadata;
import com.dbn.database.common.metadata.def.DBTypeAttributeMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DBTypeAttributeMetadataImpl extends DBObjectMetadataBase implements DBTypeAttributeMetadata {
    private final DBDataTypeMetadata dataType;

    public DBTypeAttributeMetadataImpl(ResultSet resultSet) {
        super(resultSet);
        dataType = new DBDataTypeMetadataImpl(resultSet);
    }

    @Override
    public String getAttributeName() throws SQLException {
        return getString("ATTRIBUTE_NAME");
    }

    @Override
    public String getTypeName() throws SQLException {
        return getString("TYPE_NAME");
    }

    @Override
    public short getPosition() throws SQLException {
        return resultSet.getShort("POSITION");
    }

    public DBDataTypeMetadata getDataType() {
        return dataType;
    }
}
