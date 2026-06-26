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

package com.dbn.object.impl;

import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.def.DBDataSourceConfigEntryMetadata;
import com.dbn.object.DBDataSourceConfigEntry;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBRootObjectImpl;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

@Getter
public class DBDataSourceConfigEntryImpl extends DBRootObjectImpl<DBDataSourceConfigEntryMetadata> implements DBDataSourceConfigEntry {
    private String lastUpdated;
    private String value;

    public DBDataSourceConfigEntryImpl(@NotNull ConnectionHandler connection, @NotNull String name, @NotNull String value) {
        super(connection, DBObjectType.DATA_SOURCE_CONFIG_ENTRY, name);
        this.lastUpdated = "";
        this.value = value;
    }

    DBDataSourceConfigEntryImpl(ConnectionHandler connection, DBDataSourceConfigEntryMetadata metadata) throws SQLException {
        super(connection, metadata);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBDataSourceConfigEntryMetadata metadata) throws SQLException {
        lastUpdated = metadata.getLastUpdated();
        return metadata.getKey();
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.DATA_SOURCE_CONFIG_ENTRY;
    }
}
