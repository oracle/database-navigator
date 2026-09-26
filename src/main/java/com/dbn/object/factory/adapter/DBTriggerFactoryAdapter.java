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
import com.dbn.object.type.DBTriggerTarget;
import com.dbn.object.type.DBTriggerType;

import java.sql.SQLException;
import java.util.List;

import static com.dbn.common.Priority.HIGHEST;
import static com.dbn.common.util.Strings.isEmptyOrSpaces;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.event.ObjectChangeAction.CREATE;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_BODY;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_BODY_TEMPLATE;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_EVENTS;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_FUNCTION_NAME;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_TARGET;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_TARGET_DATASET;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_TARGET_SCHEMA;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_TYPE;
import static com.dbn.object.type.DBObjectType.DATASET_TRIGGER;
import static com.dbn.object.type.DBObjectType.FUNCTION;
import static com.dbn.object.type.DBTriggerTarget.SCHEMA;

public abstract class DBTriggerFactoryAdapter implements ObjectFactoryAdapter {
    @Override
    public DBObjectSpec createInput(DatabaseEntity parentEntity) {
        DBObjectSpec input = new DBObjectSpec(parentEntity, getObjectType());
        input.setObjectName("new_trigger");
        if (input.supports(TRIGGER_FUNCTION_NAME)) {
            input.setAttributeValue(TRIGGER_FUNCTION_NAME, "new_trigger_function");
        }
        if (input.supports(TRIGGER_TYPE)) {
            input.setAttributeValue(TRIGGER_TYPE, DBTriggerType.BEFORE);
        }
        DBTriggerEvent[] defaultEvents = getDefaultEvents();
        List<DBTriggerEvent> supportedEvents = input.getSupportedValues(TRIGGER_EVENTS);
        if (!supportedEvents.containsAll(List.of(defaultEvents)) && !supportedEvents.isEmpty()) {
            defaultEvents = new DBTriggerEvent[]{supportedEvents.get(0)};
        }
        input.setAttributeValues(TRIGGER_EVENTS, defaultEvents);

        if (input.supports(TRIGGER_TARGET)) {
            DBTriggerTarget triggerTarget = getObjectType() == DATASET_TRIGGER ?
                    DBTriggerTarget.DATASET :
                    DBTriggerTarget.DATABASE;
            input.setAttributeValue(TRIGGER_TARGET, triggerTarget);
        }
        if (input.supports(TRIGGER_TARGET_SCHEMA)) {
            input.setAttributeValue(TRIGGER_TARGET_SCHEMA, parentEntity.getSchemaName());
        }

        List<String> bodyTemplates = input.getSupportedValues(TRIGGER_BODY_TEMPLATE);
        input.setAttributeValue(TRIGGER_BODY, bodyTemplates.isEmpty() ? "" : bodyTemplates.get(0));
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
        if (isEmptyOrSpaces(objectName)) {
            errors.add(txt("msg.objects.error.ObjectNameNotSpecified", objectType.getDisplayName()));
        } else if (!Strings.isWord(objectName.trim())) {
            errors.add(txt("msg.objects.error.ObjectNameInvalid", objectType.getDisplayName(), objectName));
        }

        if (input.requires(TRIGGER_TARGET_DATASET) && isEmptyOrSpaces(TRIGGER_TARGET_DATASET.value(input))) {
            errors.add(txt("msg.objects.error.SelectObject", txt("app.object.label.TriggerTargetDataset")));
        }
        if (input.requires(TRIGGER_TYPE) && TRIGGER_TYPE.value(input) == null) {
            errors.add(txt("msg.objects.error.SelectObject", txt("app.object.label.TriggerType")));
        }
        List<DBTriggerTarget> supportedTargets = input.getSupportedValues(TRIGGER_TARGET);
        if (input.supports(TRIGGER_TARGET)) {
            DBTriggerTarget triggerTarget = TRIGGER_TARGET.value(input);
            if (!supportedTargets.contains(triggerTarget)) {
                errors.add(txt("msg.objects.error.SelectObject", txt("app.objects.property.TriggerTarget")));
            } else if (triggerTarget == SCHEMA && input.requires(TRIGGER_TARGET_SCHEMA) &&
                    isEmptyOrSpaces(TRIGGER_TARGET_SCHEMA.value(input))) {
                errors.add(txt("msg.shared.error.SelectTargetSchema"));
            }
        }
        if (input.requires(TRIGGER_FUNCTION_NAME)) {
            String functionName = TRIGGER_FUNCTION_NAME.value(input);
            if (isEmptyOrSpaces(functionName)) {
                errors.add(txt("msg.objects.error.TriggerFunctionNameRequired"));
            } else if (!Strings.isWord(functionName.trim())) {
                errors.add(txt("msg.objects.error.ValidTriggerFunctionNameRequired"));
            }
        }
        DBTriggerEvent[] events = TRIGGER_EVENTS.values(input);
        if (input.requires(TRIGGER_EVENTS) && events.length == 0) {
            errors.add(txt("msg.objects.error.TriggerEventRequired"));
        }
        if (input.requires(TRIGGER_BODY) && isEmptyOrSpaces(TRIGGER_BODY.value(input))) {
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
        if (input.requires(TRIGGER_FUNCTION_NAME)) {
            ObjectChangeEvent.notify(CREATE, FUNCTION, connectionId, schemaId);
        }
    }

    protected abstract DBTriggerEvent[] getDefaultEvents();
}
