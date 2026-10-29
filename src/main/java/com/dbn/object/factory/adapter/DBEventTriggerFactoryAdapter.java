/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.object.factory.adapter;

import com.dbn.connection.DatabaseEntity;
import com.dbn.connection.SchemaId;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.type.DBObjectType;
import com.dbn.object.type.DBTriggerEvent;

import static com.dbn.object.type.DBObjectType.EVENT_TRIGGER;
import static com.dbn.object.type.DBTriggerEvent.DDL;
import static com.dbn.object.type.DBTriggerEvent.DROP;
import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_FUNCTION_SCHEMA;

public class DBEventTriggerFactoryAdapter extends DBTriggerFactoryAdapter {
    @Override
    public DBObjectType getObjectType() {
        return EVENT_TRIGGER;
    }

    @Override
    public DBObjectSpec createInput(DatabaseEntity parentEntity) {
        DBObjectSpec input = super.createInput(parentEntity);
        SchemaId schemaId = parentEntity.getConnection().getDefaultSchemaId();
        input.setSchemaId(schemaId);
        if (schemaId != null && input.supports(TRIGGER_FUNCTION_SCHEMA)) {
            input.setAttributeValue(TRIGGER_FUNCTION_SCHEMA, schemaId.getName());
        }
        return input;
    }

    @Override
    protected DBTriggerEvent[] getDefaultEvents() {
        return new DBTriggerEvent[]{DDL, DROP};
    }
}
