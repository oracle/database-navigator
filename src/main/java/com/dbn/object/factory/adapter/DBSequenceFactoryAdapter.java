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
import com.dbn.object.factory.model.DBObjectAttributeType;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.factory.ui.DBSequenceFactoryInputForm;
import com.dbn.object.type.DBObjectType;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.List;

import static com.dbn.common.Priority.HIGHEST;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.event.ObjectChangeAction.CREATE;
import static com.dbn.object.factory.model.DBObjectAttributeType.SEQUENCE_CACHE_SIZE;
import static com.dbn.object.factory.model.DBObjectAttributeType.SEQUENCE_CYCLE;
import static com.dbn.object.factory.model.DBObjectAttributeType.SEQUENCE_INCREMENT_BY;
import static com.dbn.object.factory.model.DBObjectAttributeType.SEQUENCE_MAX_VALUE;
import static com.dbn.object.factory.model.DBObjectAttributeType.SEQUENCE_MIN_VALUE;
import static com.dbn.object.factory.model.DBObjectAttributeType.SEQUENCE_START_WITH;
import static com.dbn.object.type.DBObjectType.SEQUENCE;

public class DBSequenceFactoryAdapter implements ObjectFactoryAdapter {
    @Override
    public DBObjectType getObjectType() {
        return SEQUENCE;
    }

    @Override
    public DBObjectSpec createInput(DBSchema schema) {
        DBObjectSpec input = new DBObjectSpec(schema, SEQUENCE);
        input.setObjectName("new_sequence");
        input.setAttributeValue(SEQUENCE_START_WITH, "1");
        input.setAttributeValue(SEQUENCE_INCREMENT_BY, "1");
        input.setAttributeValue(SEQUENCE_CYCLE, false);
        return input;
    }

    @Override
    public DBSequenceFactoryInputForm createInputForm(DBNComponent parent, DBObjectSpec input) {
        return new DBSequenceFactoryInputForm(parent, input);
    }

    @Override
    public void validateInput(DBObjectSpec input, List<String> errors) {
        String objectName = input.getObjectName();
        if (Strings.isEmptyOrSpaces(objectName)) {
            errors.add(txt("msg.objects.error.ObjectNameNotSpecified", SEQUENCE.getDisplayName()));
        } else if (!Strings.isWord(objectName.trim())) {
            errors.add(txt("msg.objects.error.ObjectNameInvalid", SEQUENCE.getDisplayName(), objectName));
        }

        validateInteger(input, SEQUENCE_START_WITH, "app.object.label.SequenceStartWith", errors);
        validateInteger(input, SEQUENCE_INCREMENT_BY, "app.object.label.SequenceIncrementBy", errors);
        validateInteger(input, SEQUENCE_MIN_VALUE, "app.object.label.SequenceMinValue", errors);
        validateInteger(input, SEQUENCE_MAX_VALUE, "app.object.label.SequenceMaxValue", errors);
        validateInteger(input, SEQUENCE_CACHE_SIZE, "app.object.label.SequenceCacheSize", errors);

        String increment = SEQUENCE_INCREMENT_BY.of(input);
        if (isInteger(increment) && BigInteger.ZERO.equals(new BigInteger(increment.trim()))) {
            errors.add(txt("msg.objects.error.SequenceIncrementInvalid"));
        }

        String cacheSize = SEQUENCE_CACHE_SIZE.of(input);
        if (isInteger(cacheSize) && new BigInteger(cacheSize.trim()).signum() <= 0) {
            errors.add(txt("msg.objects.error.SequenceCacheInvalid"));
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
                    dataDefinition.createSequence(input, conn);
                });

        ObjectChangeEvent.notify(CREATE, SEQUENCE, connectionId, schemaId);
    }

    private static void validateInteger(DBObjectSpec input, DBObjectAttributeType<String> attribute,
                                        String labelKey, List<String> errors) {
        String value = attribute.of(input);
        if (Strings.isEmptyOrSpaces(value) || isInteger(value)) return;

        errors.add(txt("msg.objects.error.SequenceValueInvalid", txt(labelKey)));
    }

    private static boolean isInteger(String value) {
        return !Strings.isEmptyOrSpaces(value) && value.trim().matches("-?\\d+");
    }

}
