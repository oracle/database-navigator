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

package com.dbn.object.factory.adapter;

import com.dbn.common.ui.component.DBNComponent;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.nls.NlsSupport;
import com.dbn.object.DBSchema;
import com.dbn.object.event.ObjectChangeEvent;
import com.dbn.object.factory.ObjectFactoryAdapter;
import com.dbn.object.factory.model.DBTableFactoryInput;
import com.dbn.object.factory.ui.DBTableFactoryInputForm;
import com.dbn.object.type.DBObjectType;

import java.sql.SQLException;

import static com.dbn.common.Priority.HIGHEST;
import static com.dbn.common.constant.Constant.array;
import static com.dbn.object.event.ObjectChangeAction.CREATE;
import static com.dbn.object.type.DBObjectType.TABLE;

public class DBTableFactoryAdapter implements ObjectFactoryAdapter<DBTableFactoryInput, DBTableFactoryInputForm>, NlsSupport {
    private static final DBObjectType[] OBJECT_TYPES = array(TABLE);

    @Override
    public DBObjectType[] getObjectTypes() {
        return OBJECT_TYPES;
    }

    public DBTableFactoryInput createInput(DBSchema schema, DBObjectType objectType) {
        return new DBTableFactoryInput(schema);
    }

    public DBTableFactoryInputForm createInputForm(DBNComponent parent, DBTableFactoryInput input) {
        return new DBTableFactoryInputForm(parent, input);
    }

    @Override
    public void createObject(DBTableFactoryInput input) throws SQLException {
        DBSchema schema = input.getSchema();

        ConnectionId connectionId = schema.getConnectionId();
        SchemaId schemaId = schema.getSchemaId();

        DatabaseInterfaceInvoker.execute(HIGHEST,
                "Creating " + input.getObjectType().getTitleCasedName(),
                "Creating " + input.getObjectDescription(),
                schema.getProject(),
                connectionId,
                schemaId,
                conn -> {
                    DatabaseDataDefinitionInterface dataDefinition = schema.getDataDefinitionInterface();
                    dataDefinition.createTable(input, conn);
                });

        ObjectChangeEvent.notify(CREATE, TABLE, connectionId, schemaId);
    }
}
