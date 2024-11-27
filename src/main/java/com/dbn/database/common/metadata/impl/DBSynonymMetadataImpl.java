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
import com.dbn.database.common.metadata.def.DBSynonymMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DBSynonymMetadataImpl extends DBObjectMetadataBase implements DBSynonymMetadata {

    public DBSynonymMetadataImpl(ResultSet resultSet) {
        super(resultSet);
    }

    public String getSynonymName() throws SQLException {
        return getString("SYNONYM_NAME");
    }

    @Override
    public String getUnderlyingObjectOwner() throws SQLException {
        return getString("OBJECT_OWNER");
    }

    @Override
    public String getUnderlyingObjectName() throws SQLException {
        return getString("OBJECT_NAME");
    }

    @Override
    public String getUnderlyingObjectType() throws SQLException {
        return getString("OBJECT_TYPE");
    }

    @Override
    public boolean isValid() throws SQLException {
        return isYesFlag("IS_VALID");
    }
}
