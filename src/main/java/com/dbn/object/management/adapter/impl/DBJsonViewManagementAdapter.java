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

package com.dbn.object.management.adapter.impl;

import com.dbn.common.constant.Constant;
import com.dbn.common.exception.Exceptions;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.object.DBJsonView;
import com.dbn.object.management.ObjectManagementAdapterBase;
import com.dbn.object.management.ObjectManagementAdapterExtension;
import com.dbn.object.management.ObjectManagementRequest;
import com.dbn.object.type.DBObjectType;

import java.sql.SQLException;

import static com.dbn.object.type.DBObjectType.JSON_VIEW;

/**
 * Implementation of {@link ObjectManagementAdapterExtension} for objects of type {@link DBJsonView}
 * @author Dan Cioca (Oracle)
 */
public class DBJsonViewManagementAdapter extends ObjectManagementAdapterBase<DBJsonView> {
    @Override
    public DBObjectType[] getObjectTypes() {
        return Constant.array(JSON_VIEW);
    }

    @Override
    protected void createObject(ObjectManagementRequest<DBJsonView> request) throws SQLException {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    protected void updateObject(ObjectManagementRequest<DBJsonView> request) throws SQLException {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    protected void deleteObject(ObjectManagementRequest<DBJsonView> request) throws SQLException {
        DBJsonView object = request.object();

        DatabaseDataDefinitionInterface databaseInterface = request.getDataDefinitionInterface();
        databaseInterface.dropObject(
                "VIEW",
                object.getSchemaName(true),
                object.getName(true),
                request.conn());
    }

    @Override
    protected void enableObject(ObjectManagementRequest<DBJsonView> request) throws SQLException {
        Exceptions.unsupported();
    }

    @Override
    protected void disableObject(ObjectManagementRequest<DBJsonView> request) throws SQLException {
        Exceptions.unsupported();
    }
}
