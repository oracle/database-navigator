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
import com.dbn.editor.DatabaseFileEditorManager;
import com.dbn.object.DBMethod;
import com.dbn.object.DBSchema;
import com.dbn.object.event.ObjectChangeEvent;
import com.dbn.object.factory.ObjectFactoryAdapter;
import com.dbn.object.factory.ObjectFactoryAdapters;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.factory.ui.DBMethodFactoryInputForm;
import com.dbn.object.type.DBObjectType;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dbn.common.Priority.HIGHEST;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.event.ObjectChangeAction.CREATE;
import static com.dbn.object.factory.model.DBObjectAttributeType.DATA_TYPE;
import static com.dbn.object.factory.model.DBObjectAttributeType.RETURN_ARGUMENT;
import static com.dbn.object.type.DBObjectType.ARGUMENT;

public abstract class DBMethodFactoryAdapter implements ObjectFactoryAdapter {

    public DBMethodFactoryInputForm createInputForm(DBNComponent parent, DBObjectSpec input) {
        return new DBMethodFactoryInputForm(parent, input);
    }

    @Override
    public void validateInput(DBObjectSpec input, List<String> errors) {
        String objectName = input.getObjectName();
        if (objectName.isEmpty()) {
            String hint = input.getParent() == null ? "" : " at index " + input.getIndex();
            errors.add(input.getObjectType().getName() + " name is not specified" + hint);

        } else if (!Strings.isWord(objectName)) {
            errors.add("invalid " + input.getObjectType().getName() + " name specified" + ": \"" + objectName + "\"");
        }


        DBObjectSpec returnArgument = RETURN_ARGUMENT.of(input);
        if (returnArgument != null) {
            String dataType = DATA_TYPE.of(returnArgument);
            if (Strings.isEmpty(dataType)){
                errors.add("missing data type for return argument");
            }
        }

        DBArgumentFactoryAdapter argumentAdapter = ObjectFactoryAdapters.get(ARGUMENT);
        Set<String> argumentNames = new HashSet<>();
        for (DBObjectSpec argumentInput : input.getChildren(ARGUMENT)) {
            argumentAdapter.validateInput(argumentInput, errors);
            String argumentName = argumentInput.getObjectName();
            if (Strings.isEmptyOrSpaces(argumentName)) continue; // already covered by field validator

            if (argumentNames.contains(argumentName)) {
                String hint = input.getParent() == null ? "" : " for " + input.getObjectType().getName() + " \"" + objectName + "\"";
                errors.add("duplicate argument name \"" + argumentName + "\"" + hint);
            }
            argumentNames.add(argumentName);
        }
    }

    @Override
    public void createObject(DBObjectSpec input) throws SQLException {
        DBObjectType objectType = input.getObjectType();
        String objectName = input.getObjectName();
        DBSchema schema = input.getSchema();

        ConnectionId connectionId = schema.getConnectionId();
        SchemaId schemaId = schema.getSchemaId();

        DatabaseInterfaceInvoker.execute(HIGHEST,
                txt("prc.object.title.CreatingObject", input.getObjectType().getTitleCasedName()),
                txt("prc.object.text.CreatingObjectDescription", input.getObjectDescription()),
                schema.getProject(),
                connectionId,
                schemaId,
                conn -> {
                    DatabaseDataDefinitionInterface dataDefinition = schema.getDataDefinitionInterface();
                    dataDefinition.createMethod(input, conn);
                });

        ObjectChangeEvent.notify(CREATE, objectType, connectionId, schemaId);

        DBMethod method = schema.getChildObject(objectType, objectName, false);
        if (method == null) return;

        DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(input.getProject());
        editorManager.connectAndOpenEditor(method, null, false, true);
    }
}
