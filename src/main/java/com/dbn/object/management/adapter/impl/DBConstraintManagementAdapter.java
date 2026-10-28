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

package com.dbn.object.management.adapter.impl;

import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.object.DBConstraint;
import com.dbn.object.management.ObjectManagementAdapterBase;
import com.dbn.object.management.ObjectManagementAdapterExtension;
import com.dbn.object.management.ObjectManagementRequest;
import com.dbn.object.type.DBObjectType;

import java.sql.SQLException;

import static com.dbn.common.constant.Constant.array;
import static com.dbn.object.management.ObjectManagementRequest.NEW_NAME;
import static com.dbn.object.type.DBObjectType.CONSTRAINT;

/**
 * Implementation of {@link ObjectManagementAdapterExtension} for objects of type {@link com.dbn.object.DBConstraint}
 * @author Dan Cioca (Oracle)
 */
public class DBConstraintManagementAdapter extends ObjectManagementAdapterBase<DBConstraint> {

    @Override
    public DBObjectType[] getObjectTypes() {
        return array(CONSTRAINT);
    }

    @Override
    protected void renameObject(ObjectManagementRequest<DBConstraint> request) throws SQLException {
        DBConstraint object = request.object();

        DatabaseDataDefinitionInterface databaseInterface = request.getDataDefinitionInterface();
        databaseInterface.renameConstraint(
                object.getSchemaName(),
                object.getDataset().getName(),
                object.getName(),
                request.requireAttribute(NEW_NAME),
                request.conn());
    }

    @Override
    protected void createObject(ObjectManagementRequest<DBConstraint> request) throws SQLException {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    protected void updateObject(ObjectManagementRequest<DBConstraint> request) throws SQLException {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    protected void deleteObject(ObjectManagementRequest<DBConstraint> request) throws SQLException {
        DBConstraint object = request.object();

        DatabaseDataDefinitionInterface databaseInterface = request.getDataDefinitionInterface();
        databaseInterface.dropConstraint(
                object.getSchemaName(true),
                object.getDataset().getName(true),
                object.getName(true),
                object.getConstraintType(),
                request.conn());
    }

    @Override
    protected void enableObject(ObjectManagementRequest<DBConstraint> request) throws SQLException {
        DBConstraint object = request.object();

        DatabaseMetadataInterface databaseInterface = request.getMetadataInterface();
        databaseInterface.enableConstraint(
                object.getSchemaName(true),
                object.getDataset().getName(true),
                object.getName(true),
                request.conn());
    }

    @Override
    protected void disableObject(ObjectManagementRequest<DBConstraint> request) throws SQLException {
        DBConstraint object = request.object();

        DatabaseMetadataInterface databaseInterface = request.getMetadataInterface();
        databaseInterface.disableConstraint(
                object.getSchemaName(true),
                object.getDataset().getName(true),
                object.getName(true),
                request.conn());
    }
}
