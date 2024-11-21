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
import com.dbn.database.common.metadata.def.DBSchemaMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DBSchemaMetadataImpl extends DBObjectMetadataBase implements DBSchemaMetadata {
    public DBSchemaMetadataImpl(ResultSet resultSet) {
        super(resultSet);
    }

    public String getSchemaName() throws SQLException {
        return getString("SCHEMA_NAME");
    }

    public boolean isPublic() throws SQLException {
        return isYesFlag("IS_PUBLIC");
    }

    public boolean isSystem() throws SQLException {
        return isYesFlag("IS_SYSTEM");
    }

    public boolean isEmpty() throws SQLException {
        return isYesFlag("IS_EMPTY");
    }

}

