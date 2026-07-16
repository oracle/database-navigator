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
import com.dbn.database.common.metadata.def.DBDatasourceConfigMetadata;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBDatasourceConfig;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObjectImpl;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static com.dbn.common.Priority.HIGHEST;

@Getter
public class DBDatasourceConfigImpl extends DBSchemaObjectImpl<DBDatasourceConfigMetadata> implements DBDatasourceConfig {
    private String lastUpdated;
    private String value;

    public DBDatasourceConfigImpl(
            @NotNull DBSchema schema,
            @NotNull String configName,
            @NotNull String value) throws SQLException {
        super(schema, DBDatasourceConfigMetadata.Record.builder()
                .ownerName(schema.getName())
                .configName(configName)
                .lastUpdated("")
                .build());
        this.lastUpdated = "";
        this.value = value;
    }

    DBDatasourceConfigImpl(DBSchema schema, DBDatasourceConfigMetadata metadata) throws SQLException {
        super(schema, metadata);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBDatasourceConfigMetadata metadata) throws SQLException {
        lastUpdated = metadata.getLastUpdated();
        return metadata.getConfigName();
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.DATASOURCE_CONFIG;
    }

    @Override
    public void executeUpdateDDL(DBContentType contentType, String oldCode, String newCode) throws SQLException {
        DatabaseInterfaceInvoker.execute(HIGHEST, getProject(), getConnectionId(),
                conn -> getConnection().getDatasourceConfigInterface().updateDatasourceConfig(
                        getSchema().getName(), getName(), newCode, conn));
        value = newCode;
    }

}
