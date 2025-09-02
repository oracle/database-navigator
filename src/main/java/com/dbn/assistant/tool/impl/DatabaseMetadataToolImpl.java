/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.assistant.tool.impl;

import com.dbn.assistant.tool.AssistantToolBase;
import com.dbn.assistant.tool.spec.DatabaseMetadataTool;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.info.ConnectionInfo;

public class DatabaseMetadataToolImpl extends AssistantToolBase implements DatabaseMetadataTool {

    @Override
    public DatabaseInformation loadDatabaseInformation() {
        ConnectionInfo connectionInfo = getConnection().getConnectionInfo();
        if (connectionInfo == null) throw new IllegalStateException("Could not connect to database");

        DatabaseType databaseType = connectionInfo.getDatabaseType();
        DatabaseInformation information = new DatabaseInformation();

        information.setType(databaseType.id());
        information.setName(connectionInfo.getProductName());
        information.setVersion(connectionInfo.getProductVersion());
        return information;

    }

}
