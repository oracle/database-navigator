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
import com.dbn.connection.DatabaseEntity;
import com.dbn.connection.SchemaId;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.object.DBSchema;
import com.dbn.object.event.ObjectChangeEvent;
import com.dbn.object.factory.ObjectFactoryAdapter;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.factory.ui.DBSynonymFactoryInputForm;
import com.dbn.object.type.DBObjectType;

import java.sql.SQLException;
import java.util.List;

import static com.dbn.common.Priority.HIGHEST;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.event.ObjectChangeAction.CREATE;
import static com.dbn.object.factory.model.DBObjectAttributeType.SYNONYM_TARGET_OBJECT_NAME;
import static com.dbn.object.factory.model.DBObjectAttributeType.SYNONYM_TARGET_OBJECT_TYPE;
import static com.dbn.object.factory.model.DBObjectAttributeType.SYNONYM_TARGET_SCHEMA;
import static com.dbn.object.type.DBObjectType.SYNONYM;

public class DBSynonymFactoryAdapter implements ObjectFactoryAdapter {
    @Override
    public DBObjectType getObjectType() {
        return SYNONYM;
    }

    @Override
    public DBObjectSpec createInput(DatabaseEntity parentEntity) {
        DBObjectSpec input = new DBObjectSpec(parentEntity, SYNONYM);
        input.setObjectName("new_synonym");
        input.setAttributeValue(SYNONYM_TARGET_SCHEMA, parentEntity.getName());
        input.setAttributeValue(SYNONYM_TARGET_OBJECT_TYPE, DBObjectType.TABLE);
        return input;
    }

    @Override
    public DBSynonymFactoryInputForm createInputForm(DBNComponent parent, DBObjectSpec input) {
        return new DBSynonymFactoryInputForm(parent, input);
    }

    @Override
    public void validateInput(DBObjectSpec input, List<String> errors) {
        String objectName = input.getObjectName();
        if (Strings.isEmptyOrSpaces(objectName)) {
            errors.add(txt("msg.objects.error.ObjectNameNotSpecified", SYNONYM.getDisplayName()));
        } else if (!Strings.isWord(objectName.trim())) {
            errors.add(txt("msg.objects.error.ObjectNameInvalid", SYNONYM.getDisplayName(), objectName));
        }

        if (Strings.isEmptyOrSpaces(SYNONYM_TARGET_SCHEMA.value(input))) {
            errors.add(txt("msg.shared.error.SelectTargetSchema"));
        }
        if (SYNONYM_TARGET_OBJECT_TYPE.value(input) == null) {
            errors.add(txt("msg.objects.error.SelectObject", txt("app.object.label.TargetObjectType")));
        }
        if (Strings.isEmptyOrSpaces(SYNONYM_TARGET_OBJECT_NAME.value(input))) {
            errors.add(txt("msg.objects.error.SelectObject", txt("app.object.label.TargetObject")));
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
                    dataDefinition.createSynonym(input, conn);
                });

        ObjectChangeEvent.notify(CREATE, SYNONYM, connectionId, schemaId);
    }
}
