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
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.object.DBSchema;
import com.dbn.object.event.ObjectChangeEvent;
import com.dbn.object.factory.ObjectFactoryAdapter;
import com.dbn.object.factory.ObjectFactoryAdapters;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.factory.model.DBObjectSpecList;
import com.dbn.object.factory.ui.DBTableFactoryInputForm;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.Nls;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dbn.common.Priority.HIGHEST;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.event.ObjectChangeAction.CREATE;
import static com.dbn.object.type.DBObjectType.COLUMN;
import static com.dbn.object.type.DBObjectType.TABLE;

public class DBTableFactoryAdapter implements ObjectFactoryAdapter {

    @Override
    public DBObjectType getObjectType() {
        return TABLE;
    }

    public DBObjectSpec createInput(DBSchema schema) {
        return new DBObjectSpec(schema, TABLE);
    }

    public DBTableFactoryInputForm createInputForm(DBNComponent parent, DBObjectSpec tableSpec) {
        return new DBTableFactoryInputForm(parent, tableSpec);
    }

    @Override
    public void validateInput(DBObjectSpec tableSpec, List<@Nls String> errors) {
        String objectName = tableSpec.getObjectName();
        DBObjectType objectType = tableSpec.getObjectType();

        if (objectName.isEmpty()) {
            errors.add(tableSpec.getParent() == null ?
                    txt("msg.objects.error.ObjectNameNotSpecified", objectType.getDisplayName()) :
                    txt("msg.objects.error.ObjectNameNotSpecifiedAtIndex", objectType.getDisplayName(), tableSpec.getIndex()));

        } else if (!Strings.isWord(objectName)) {
            errors.add(txt("msg.objects.error.ObjectNameInvalid", objectType.getDisplayName(), objectName));
        }

        DBColumnFactoryAdapter columnAdapter = ObjectFactoryAdapters.get(COLUMN);
        Set<String> columnNames = new HashSet<>();
        for (DBObjectSpec columnSpec : tableSpec.getChildren(DBObjectType.COLUMN)) {
            columnAdapter.validateInput(columnSpec, errors);
            String columnName = columnSpec.getObjectName();
            if (Strings.isEmptyOrSpaces(columnName)) continue; // already covered by field validator

            if (columnNames.contains(columnName)) {
                errors.add(tableSpec.getParent() == null ?
                        txt("msg.objects.error.DuplicateColumnName", columnName) :
                        txt("msg.objects.error.DuplicateColumnNameForObject", columnName, objectType.getDisplayName(), objectName));
            }
            columnNames.add(columnName);
        }
    }

    @Override
    public void createObject(DBObjectSpec input) throws SQLException {
        DBObjectType objectType = input.getObjectType();
        DBSchema schema = input.getSchema();

        ConnectionId connectionId = schema.getConnectionId();
        SchemaId schemaId = schema.getSchemaId();

        DatabaseInterfaceInvoker.execute(HIGHEST,
                txt("prc.object.title.CreatingObject", objectType.getTitleCasedDisplayName()),
                txt("prc.object.text.CreatingObjectDescription", input.getObjectDescription()),
                schema.getProject(),
                connectionId,
                schemaId,
                conn -> {
                    DatabaseDataDefinitionInterface dataDefinition = schema.getDataDefinitionInterface();
                    dataDefinition.createTable(input, conn);
                    DBObjectSpecList indexSpecs = input.getChildren(DBObjectType.INDEX);
                    for (DBObjectSpec indexSpec : indexSpecs) {
                        dataDefinition.createIndex(indexSpec, conn);
                    }
                });

        ObjectChangeEvent.notify(CREATE, TABLE, connectionId, schemaId);
    }
}
