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

import com.dbn.browser.ui.HtmlToolTipBuilder;
import com.dbn.connection.ConnectionHandler;
import com.dbn.data.type.DBDataType;
import com.dbn.database.common.metadata.DBObjectMetadata;
import com.dbn.object.DBColumn;
import com.dbn.object.DBConstraint;
import com.dbn.object.DBDataset;
import com.dbn.object.DBDatasetTrigger;
import com.dbn.object.DBIndex;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBSchemaObjectImpl;
import com.dbn.object.common.list.DBObjectListContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;

import static com.dbn.object.type.DBObjectRelationType.CONSTRAINT_COLUMN;
import static com.dbn.object.type.DBObjectType.COLUMN;
import static com.dbn.object.type.DBObjectType.CONSTRAINT;
import static com.dbn.object.type.DBObjectType.DATASET_TRIGGER;

abstract class DBDatasetImpl<M extends DBObjectMetadata> extends DBSchemaObjectImpl<M> implements DBDataset {
    DBDatasetImpl(DBSchema parent, M metadata) throws SQLException {
        super(parent, metadata);
    }

    @Override
    protected void initLists(ConnectionHandler connection) {
        super.initLists(connection);
        DBSchema schema = getSchema();
        DBObjectListContainer childObjects = ensureChildObjects();

        childObjects.createSubcontentObjectList(COLUMN, this, schema);
        childObjects.createSubcontentObjectList(CONSTRAINT, this, schema);
        childObjects.createSubcontentObjectList(DATASET_TRIGGER, this, schema);
        childObjects.createSubcontentObjectRelationList(CONSTRAINT_COLUMN, this, schema);
    }

    @Override
    @NotNull
    public List<DBColumn> getColumns() {
        return getChildObjects(COLUMN);
    }

    @Override
    @Nullable
    public List<DBConstraint> getConstraints() {
        return getChildObjects(CONSTRAINT);
    }

    @Override
    @Nullable
    public List<DBDatasetTrigger> getTriggers() {
        return getChildObjects(DATASET_TRIGGER);
    }

    @Override
    @Nullable
    public DBColumn getColumn(String name) {
        return getChildObject(COLUMN, name);
    }

    @Override
    @Nullable
    public DBConstraint getConstraint(String name) {
        return getChildObject(CONSTRAINT, name);
    }

    @Override
    @Nullable
    public DBDatasetTrigger getTrigger(String name) {
        return getChildObject(DATASET_TRIGGER, name);
    }

    @Nullable
    @Override
    public List<DBIndex> getIndexes() {
        return null;
    }

    @Nullable
    @Override
    public DBIndex getIndex(String name) {
        return null;
    }

    @Override
    public boolean hasLobColumns() {
        for (DBColumn column : getColumns()) {
            DBDataType dataType = column.getDataType();
            if (dataType.isNative() && dataType.getNativeType().isLargeObject()) {
                return true;
            }

        }
        return false;
    }

    @Override
    public void buildToolTip(HtmlToolTipBuilder ttb) {
        ttb.append(true, getObjectType().getName(), true);
        ttb.createEmptyRow();
        super.buildToolTip(ttb);
    }

}
