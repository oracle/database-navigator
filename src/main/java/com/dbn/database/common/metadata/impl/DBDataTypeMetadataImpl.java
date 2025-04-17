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

import com.dbn.common.latent.Latent;
import com.dbn.database.common.metadata.DBObjectMetadataBase;
import com.dbn.database.common.metadata.def.DBDataTypeMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DBDataTypeMetadataImpl extends DBObjectMetadataBase implements DBDataTypeMetadata {
    private static final String EMPTY_PREFIX = "";
    private final String prefix;
    private final Latent<DBDataTypeMetadataImpl> collection = Latent.basic(() -> new DBDataTypeMetadataImpl(resultSet, "COLLECTION_"));

    DBDataTypeMetadataImpl(ResultSet resultSet) {
        this(resultSet, EMPTY_PREFIX);
    }

    private DBDataTypeMetadataImpl(ResultSet resultSet, String prefix) {
        super(resultSet);
        this.prefix = prefix;
    }

    public String getDataTypeName() throws SQLException {
        return getString(prefix + "DATA_TYPE_NAME");
    }

    public String getDeclaredTypeName() throws SQLException {
        return getString(prefix + "DECL_TYPE_NAME");
    }

    public String getDeclaredTypeOwner() throws SQLException {
        return getString(prefix + "DECL_TYPE_OWNER");
    }

    public String getDeclaredTypeProgram() throws SQLException {
        return getString(prefix + "DECL_TYPE_PROGRAM");
    }

    public long getDataLength() throws SQLException {
        return resultSet.getLong(prefix + "DATA_LENGTH");
    }

    public int getDataPrecision() throws SQLException {
        return resultSet.getInt(prefix + "DATA_PRECISION");
    }

    public int getDataScale() throws SQLException {
        return resultSet.getInt(prefix + "DATA_SCALE");
    }

    public boolean isSet() throws SQLException {
        return isYesFlag(prefix + "IS_SET");
    }

    public DBDataTypeMetadataImpl collection() {
        return collection.get();
    }
}
