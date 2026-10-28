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

import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.object.DBTable;
import com.dbn.object.management.ObjectManagementAdapterBase;
import com.dbn.object.management.ObjectManagementRequest;
import com.dbn.object.type.DBObjectType;

import java.sql.SQLException;

import static com.dbn.common.constant.Constant.array;
import static com.dbn.object.management.ObjectManagementRequest.NEW_NAME;
import static com.dbn.object.type.DBObjectType.TABLE;

public class DBTableManagementAdapter extends ObjectManagementAdapterBase<DBTable> {
    @Override
    public DBObjectType[] getObjectTypes() {
        return array(TABLE);
    }

    @Override
    protected void renameObject(ObjectManagementRequest<DBTable> request) throws SQLException {
        DBTable object = request.object();

        DatabaseDataDefinitionInterface databaseInterface = request.getDataDefinitionInterface();
        databaseInterface.renameTable(
                object.getSchema().getName(),
                object.getName(),
                request.requireAttribute(NEW_NAME),
                request.conn());
    }

    @Override
    protected void deleteObject(ObjectManagementRequest<DBTable> request) throws SQLException {
        DBTable object = request.object();

        DatabaseDataDefinitionInterface databaseInterface = request.getDataDefinitionInterface();
        databaseInterface.dropObject("table",
                object.getSchemaName(true),
                object.getName(true),
                request.conn());
    }
}
