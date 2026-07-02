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
import com.dbn.database.common.metadata.def.DBConnectionConfigurationMetadata;
import com.dbn.object.DBConnectionConfiguration;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBRootObjectImpl;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

@Getter
public class DBConnectionConfigurationImpl extends DBRootObjectImpl<DBConnectionConfigurationMetadata> implements DBConnectionConfiguration {
    private String ownerName;
    private String configName;
    private String lastUpdated;
    private String value;

    public DBConnectionConfigurationImpl(
            @NotNull ConnectionHandler connection,
            @NotNull String ownerName,
            @NotNull String configName,
            @NotNull String value) {
        super(connection, DBObjectType.CONNECTION_CONFIGURATION, qualifiedName(ownerName, configName));
        this.ownerName = ownerName;
        this.configName = configName;
        this.lastUpdated = "";
        this.value = value;
    }

    DBConnectionConfigurationImpl(ConnectionHandler connection, DBConnectionConfigurationMetadata metadata) throws SQLException {
        super(connection, metadata);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBConnectionConfigurationMetadata metadata) throws SQLException {
        ownerName = metadata.getOwnerName();
        configName = metadata.getConfigName();
        lastUpdated = metadata.getLastUpdated();
        return getQualifiedConfigName();
    }

    @Override
    public String getQualifiedConfigName() {
        return qualifiedName(ownerName, configName);
    }

    @Override
    public String getPresentableName() {
        return configName;
    }

    @Override
    public String getPresentableTextDetails() {
        return ownerName;
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.CONNECTION_CONFIGURATION;
    }

    private static String qualifiedName(String ownerName, String configName) {
        return ownerName + "." + configName;
    }
}
