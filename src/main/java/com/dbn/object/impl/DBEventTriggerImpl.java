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

package com.dbn.object.impl;

import com.dbn.browser.ui.HtmlToolTipBuilder;
import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.def.DBTriggerMetadata;
import com.dbn.object.DBEventTrigger;
import com.dbn.object.DBFunction;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBRootObjectImpl;
import com.dbn.object.common.status.DBObjectStatusHolder;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.dbn.object.type.DBTriggerEvent;
import com.dbn.object.type.DBTriggerTarget;
import com.dbn.object.type.DBTriggerType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.SQLException;

import static com.dbn.common.util.Strings.cachedLowerCase;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.object.common.property.DBObjectProperty.EDITABLE;
import static com.dbn.object.common.property.DBObjectProperty.FOR_EACH_ROW;
import static com.dbn.object.common.property.DBObjectProperty.ROOT_OBJECT;
import static com.dbn.object.common.status.DBObjectStatus.DEBUG;
import static com.dbn.object.common.status.DBObjectStatus.DISABLED;
import static com.dbn.object.common.status.DBObjectStatus.VALID;
import static com.dbn.object.type.DBObjectType.EVENT_TRIGGER;
import static com.dbn.object.type.DBObjectType.FUNCTION;
import static com.dbn.object.type.DBObjectType.SCHEMA;

@Getter
class DBEventTriggerImpl extends DBRootObjectImpl<DBTriggerMetadata> implements DBEventTrigger {
    private DBTriggerType triggerType;
    private DBTriggerEvent[] triggerEvents;
    private DBTriggerTarget triggerTarget;
    private @Nullable DBObjectRef<DBFunction> triggerFunction;

    DBEventTriggerImpl(ConnectionHandler connection, DBTriggerMetadata metadata) throws SQLException {
        super(connection, metadata);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBTriggerMetadata metadata) throws SQLException {
        set(FOR_EACH_ROW, metadata.isForEachRow());
        triggerTarget = DBTriggerTarget.value(metadata.getTriggerTarget());
        if (triggerTarget == DBTriggerTarget.UNKNOWN) {
            triggerTarget = DBTriggerTarget.DATABASE;
        }

        initTriggerFunction(connection, metadata);

        triggerType = DBTriggerType.value(metadata.getTriggerType());
        triggerEvents = DBTriggerEvent.values(metadata.getTriggeringEvent());
        return metadata.getTriggerName();
    }

    private void initTriggerFunction(ConnectionHandler connection, DBTriggerMetadata metadata) throws SQLException {
        String functionName = metadata.getTriggerFunctionName();
        String functionSchemaName = metadata.getTriggerFunctionSchemaName();
        if (isEmpty(functionName)) return;
        if (isEmpty(functionSchemaName)) return;

        DBObjectRef<DBSchema> schemaRef = new DBObjectRef<>(connection.getConnectionId(), SCHEMA, functionSchemaName);
        triggerFunction = new DBObjectRef<>(schemaRef, FUNCTION, functionName);
    }

    @Override
    public void initStatus(DBTriggerMetadata metadata) throws SQLException {
        setStatus(DISABLED, metadata.isDisabled());
        setStatus(VALID, metadata.isValid());
        setStatus(DEBUG, metadata.isDebug());
    }

    @Override
    protected void initProperties() {
        properties.set(ROOT_OBJECT, true);
        properties.set(EDITABLE, true);
    }

    @Override
    public boolean isForEachRow() {
        return is(FOR_EACH_ROW);
    }

    @Nullable
    @Override
    public DBFunction getTriggerFunction() {
        return DBObjectRef.get(triggerFunction);
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return EVENT_TRIGGER;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        DBObjectStatusHolder objectStatus = getStatus();
        boolean valid = objectStatus.is(VALID);
        boolean disabled = objectStatus.is(DISABLED);
        if (valid) {
            boolean debug = objectStatus.is(DEBUG);
            if (disabled) {
                return debug ?
                        Icons.DBO_DATABASE_TRIGGER_DEBUG_DISABLED :
                        Icons.DBO_DATABASE_TRIGGER_DISABLED;
            }
            return debug ?
                    Icons.DBO_DATABASE_TRIGGER_DEBUG :
                    Icons.DBO_DATABASE_TRIGGER;
        }

        return disabled ?
                Icons.DBO_DATABASE_TRIGGER_ERR_DISABLED :
                Icons.DBO_DATABASE_TRIGGER_ERR;
    }

    @Override
    public void buildToolTip(HtmlToolTipBuilder ttb) {
        ttb.append(true, getObjectType().getName(), true);
        StringBuilder triggerDescription = new StringBuilder(" - ");
        if (triggerType != DBTriggerType.UNKNOWN) {
            triggerDescription.append(cachedLowerCase(triggerType.getName())).append(' ');
        }
        for (int i = 0; i < triggerEvents.length; i++) {
            if (i > 0) triggerDescription.append(" or ");
            triggerDescription.append(triggerEvents[i].getName());
        }
        triggerDescription.append(" on ").append(triggerTarget.getName());
        ttb.append(false, triggerDescription.toString(), false);
        ttb.createEmptyRow();
        super.buildToolTip(ttb);
    }
}
