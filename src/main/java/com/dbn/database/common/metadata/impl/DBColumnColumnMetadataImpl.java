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

package com.dbn.database.common.metadata.impl;

import com.dbn.database.common.metadata.DBObjectMetadataBase;
import com.dbn.database.common.metadata.def.DBColumnColumnMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DBColumnColumnMetadataImpl extends DBObjectMetadataBase implements DBColumnColumnMetadata {
    public DBColumnColumnMetadataImpl(ResultSet resultSet) {
        super(resultSet);
    }

    @Override public String getSourceSchemaName() throws SQLException { return getString("SOURCE_SCHEMA_NAME"); }
    @Override public String getSourceDatasetName() throws SQLException { return getString("SOURCE_DATASET_NAME"); }
    @Override public String getSourceColumnName() throws SQLException { return getString("SOURCE_COLUMN_NAME"); }

    @Override public String getTargetSchemaName() throws SQLException { return getString("TARGET_SCHEMA_NAME"); }
    @Override public String getTargetDatasetName() throws SQLException { return getString("TARGET_DATASET_NAME"); }
    @Override public String getTargetColumnName() throws SQLException { return getString("TARGET_COLUMN_NAME"); }
}
