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
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.factory.ui.DBViewFactoryInputForm;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NonNls;

import java.sql.SQLException;
import java.util.List;

import static com.dbn.common.Priority.HIGHEST;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.event.ObjectChangeAction.CREATE;
import static com.dbn.object.factory.model.DBObjectAttributeType.OBJECT_DETAIL;
import static com.dbn.object.type.DBObjectType.VIEW;

public class DBViewFactoryAdapter implements ObjectFactoryAdapter {
    @Override
    public DBObjectType getObjectType() {
        return VIEW;
    }

    @Override
    public DBObjectSpec createInput(DBSchema schema) {
        DBObjectSpec input = new DBObjectSpec(schema, getObjectType());
        input.setObjectName(getObjectType() == VIEW ? "new_view" : "new_materialized_view");
        input.setAttributeValue(OBJECT_DETAIL, "");
        return input;
    }

    @Override
    public DBViewFactoryInputForm createInputForm(DBNComponent parent, DBObjectSpec input) {
        return new DBViewFactoryInputForm(parent, input);
    }

    @Override
    public void validateInput(DBObjectSpec input, List<String> errors) {
        DBObjectType objectType = input.getObjectType();
        String objectName = input.getObjectName();
        if (Strings.isEmptyOrSpaces(objectName)) {
            errors.add(txt("msg.objects.error.ObjectNameNotSpecified", objectType.getDisplayName()));
        } else if (!Strings.isWord(objectName.trim())) {
            errors.add(txt("msg.objects.error.ObjectNameInvalid", objectType.getDisplayName(), objectName));
        }

        String selectStatement = OBJECT_DETAIL.of(input);
        if (Strings.isEmptyOrSpaces(selectStatement)) {
            errors.add(txt("msg.objects.error.SelectStatementRequired"));
        }
    }

    @Override
    public void createObject(DBObjectSpec input) throws SQLException {
        DBSchema schema = input.getSchema();
        ConnectionId connectionId = schema.getConnectionId();
        SchemaId schemaId = schema.getSchemaId();

        DatabaseInterfaceInvoker.execute(
                HIGHEST,
                txt("prc.object.title.CreatingObject", input.getObjectType().getTitleCasedDisplayName()),
                txt("prc.object.text.CreatingObjectDescription", input.getObjectDescription()),
                input.getProject(),
                connectionId,
                schemaId,
                conn -> {
                    DatabaseDataDefinitionInterface dataDefinition = schema.getDataDefinitionInterface();
                    @NonNls String createStatement = input.getObjectType().getDisplayName() + " " +
                            input.getSchemaName(true) + "." +
                            input.getAdjustedObjectName() + " as\n" +
                            OBJECT_DETAIL.of(input);
                    dataDefinition.createObject(createStatement, conn);
                });

        ObjectChangeEvent.notify(CREATE, input.getObjectType(), connectionId, schemaId);
    }
}
