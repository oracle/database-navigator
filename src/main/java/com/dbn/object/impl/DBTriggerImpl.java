/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.object.impl;

import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.def.DBTriggerMetadata;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBDataset;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTrigger;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObjectImpl;
import com.dbn.object.type.DBTriggerEvent;
import com.dbn.object.type.DBTriggerTarget;
import com.dbn.object.type.DBTriggerType;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;

import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.object.common.property.DBObjectProperty.COMPILABLE;
import static com.dbn.object.common.property.DBObjectProperty.DEBUGGABLE;
import static com.dbn.object.common.property.DBObjectProperty.DISABLEABLE;
import static com.dbn.object.common.property.DBObjectProperty.EDITABLE;
import static com.dbn.object.common.property.DBObjectProperty.FOR_EACH_ROW;
import static com.dbn.object.common.property.DBObjectProperty.INVALIDABLE;
import static com.dbn.object.common.property.DBObjectProperty.REFERENCEABLE;
import static com.dbn.object.common.property.DBObjectProperty.SCHEMA_OBJECT;
import static com.dbn.object.common.status.DBObjectStatus.DEBUG;
import static com.dbn.object.common.status.DBObjectStatus.DISABLED;
import static com.dbn.object.common.status.DBObjectStatus.VALID;

@Getter
abstract class DBTriggerImpl extends DBSchemaObjectImpl<DBTriggerMetadata> implements DBTrigger {
    private DBTriggerType triggerType;
    private DBTriggerEvent[] triggerEvents;
    private DBTriggerTarget triggerTarget;
    private @Nullable DBSchema targetSchema;

    DBTriggerImpl(DBSchema schema, DBTriggerMetadata metadata) throws SQLException {
        super(schema, metadata);
    }

    DBTriggerImpl(DBDataset dataset, DBTriggerMetadata metadata) throws SQLException {
        super(dataset, metadata);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBTriggerMetadata metadata) throws SQLException {
        String name = metadata.getTriggerName();
        set(FOR_EACH_ROW, metadata.isForEachRow());
        triggerTarget = DBTriggerTarget.value(metadata.getTriggerTarget());
        if (triggerTarget == DBTriggerTarget.UNKNOWN) {
            triggerTarget = parentObject instanceof DBDataset ? DBTriggerTarget.DATASET : DBTriggerTarget.DATABASE;
        }
        String targetSchemaName = metadata.getTargetSchemaName();
        if (isNotEmpty(targetSchemaName)) {
            targetSchema = connection.getObjectBundle().getSchema(targetSchemaName);
        }
        if (targetSchema == null && triggerTarget == DBTriggerTarget.SCHEMA) {
            targetSchema = parentObject instanceof DBSchema schema ? schema :
                    parentObject instanceof DBDataset dataset ? dataset.getSchema() : null;
        }

        triggerType = DBTriggerType.value(metadata.getTriggerType());
        triggerEvents = DBTriggerEvent.values(metadata.getTriggeringEvent());

        return name;
    }

    @Override
    public void initStatus(DBTriggerMetadata metadata) throws SQLException {
        setStatus(DISABLED, metadata.isDisabled());
        setStatus(VALID, metadata.isValid());
        setStatus(DEBUG, metadata.isDebug());
    }

    @Override
    public void initProperties() {
        properties.set(EDITABLE, true);
        properties.set(DISABLEABLE, true);
        properties.set(REFERENCEABLE, true);
        properties.set(COMPILABLE, true);
        properties.set(DEBUGGABLE, true);
        properties.set(INVALIDABLE, true);
        properties.set(SCHEMA_OBJECT, true);
    }

    @Override
    public boolean isForEachRow() {
        return is(FOR_EACH_ROW);
    }

    /*********************************************************
     *                     TreeElement                       *
     *********************************************************/

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public String getCodeParseRootId(DBContentType contentType) {
        return "trigger_definition";
    }
}
