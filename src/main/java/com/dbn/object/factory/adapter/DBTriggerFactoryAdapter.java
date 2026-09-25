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
import com.dbn.object.factory.ui.DBTriggerFactoryInputForm;
import com.dbn.object.type.DBObjectType;
import com.dbn.object.type.DBTriggerEvent;
import com.dbn.object.type.DBTriggerType;

import java.sql.SQLException;
import java.util.List;

import static com.dbn.common.Priority.HIGHEST;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.event.ObjectChangeAction.CREATE;
import static com.dbn.object.factory.model.DBObjectAttributeType.OBJECT_DETAIL;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_EVENTS;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_TARGET_DATASET;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_TYPE;
import static com.dbn.object.type.DBObjectType.DATASET_TRIGGER;

public abstract class DBTriggerFactoryAdapter implements ObjectFactoryAdapter {
    @Override
    public DBObjectSpec createInput(DatabaseEntity parentEntity) {
        DBObjectSpec input = new DBObjectSpec(parentEntity, getObjectType());
        input.setObjectName("new_trigger");
        input.setAttributeValue(TRIGGER_TYPE, DBTriggerType.BEFORE);
        input.setAttributeValue(TRIGGER_EVENTS, getDefaultEvents());
        input.setAttributeValue(OBJECT_DETAIL, "begin\n    null;\nend;");
        return input;
    }

    @Override
    public DBTriggerFactoryInputForm createInputForm(DBNComponent parent, DBObjectSpec input) {
        return new DBTriggerFactoryInputForm(parent, input);
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

        if (objectType == DATASET_TRIGGER && Strings.isEmptyOrSpaces(TRIGGER_TARGET_DATASET.of(input))) {
            errors.add(txt("msg.objects.error.SelectObject", txt("app.object.label.TriggerTargetDataset")));
        }
        if (TRIGGER_TYPE.of(input) == null) {
            errors.add(txt("msg.objects.error.SelectObject", txt("app.object.label.TriggerType")));
        }
        DBTriggerEvent[] events = TRIGGER_EVENTS.of(input);
        if (events == null || events.length == 0) {
            errors.add(txt("msg.objects.error.TriggerEventRequired"));
        }
        if (Strings.isEmptyOrSpaces(OBJECT_DETAIL.of(input))) {
            errors.add(txt("msg.objects.error.TriggerBodyRequired"));
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
                    dataDefinition.createTrigger(input, conn);
                });

        ObjectChangeEvent.notify(CREATE, getObjectType(), connectionId, schemaId);
    }

    protected abstract DBTriggerEvent[] getDefaultEvents();
}
