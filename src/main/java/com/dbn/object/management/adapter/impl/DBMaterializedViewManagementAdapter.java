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

package com.dbn.object.management.adapter.impl;

import com.dbn.common.constant.Constant;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.object.DBMaterializedView;
import com.dbn.object.management.ObjectManagementAdapterBase;
import com.dbn.object.type.DBObjectType;

import java.sql.SQLException;

import static com.dbn.object.type.DBObjectType.MATERIALIZED_VIEW;

/**
 * Implementation of {@link com.dbn.object.management.ObjectManagementAdapterExtension} for materialized views.
 */
public class DBMaterializedViewManagementAdapter extends ObjectManagementAdapterBase<DBMaterializedView> {
    @Override
    public DBObjectType[] getObjectTypes() {
        return Constant.array(MATERIALIZED_VIEW);
    }

    @Override
    protected void refreshObject(ConnectionHandler connection, DBNConnection conn, DBMaterializedView object) throws SQLException {
        DatabaseDataDefinitionInterface databaseInterface = connection.getDataDefinitionInterface();
        String schemaName = object.getSchemaName(true);
        String viewName = object.getName(true);
        databaseInterface.refreshMaterializedView(schemaName, viewName, conn);
    }
}
