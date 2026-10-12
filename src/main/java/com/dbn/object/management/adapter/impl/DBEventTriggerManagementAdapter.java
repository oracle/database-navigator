/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.object.management.adapter.impl;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.object.DBEventTrigger;
import com.dbn.object.management.ObjectManagementAdapterBase;
import com.dbn.object.type.DBObjectType;

import java.sql.SQLException;

import static com.dbn.object.type.DBObjectType.EVENT_TRIGGER;

public class DBEventTriggerManagementAdapter extends ObjectManagementAdapterBase<DBEventTrigger> {
    @Override
    public DBObjectType[] getObjectTypes() {
        return new DBObjectType[]{EVENT_TRIGGER};
    }

    @Override
    protected void deleteObject(ConnectionHandler connection, DBNConnection conn, DBEventTrigger object) throws SQLException {
        DatabaseDataDefinitionInterface dataDefinition = connection.getDataDefinitionInterface();
        dataDefinition.dropEventTrigger(object.getName(true), conn);
    }
}
