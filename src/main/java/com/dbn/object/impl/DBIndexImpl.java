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
import com.dbn.database.common.metadata.def.DBIndexMetadata;
import com.dbn.object.DBColumn;
import com.dbn.object.DBDataset;
import com.dbn.object.DBIndex;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObjectImpl;
import com.dbn.object.common.list.DBObjectListContainer;
import com.dbn.object.common.list.DBObjectNavigationList;
import com.dbn.object.common.property.DBObjectProperty;
import com.dbn.object.common.status.DBObjectStatus;
import com.dbn.object.type.DBObjectRelationType;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

class DBIndexImpl extends DBSchemaObjectImpl<DBIndexMetadata> implements DBIndex {
    DBIndexImpl(DBDataset dataset, DBIndexMetadata metadata) throws SQLException {
        super(dataset, metadata);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBIndexMetadata metadata) throws SQLException {
        String name = metadata.getIndexName();
        set(DBObjectProperty.UNIQUE, metadata.isUnique());
        return name;
    }

    @Override
    public void initStatus(DBIndexMetadata metadata) throws SQLException {
        boolean valid = metadata.isValid();
        getStatus().set(DBObjectStatus.VALID, valid);
    }

    @Override
    public void initProperties() {
        properties.set(DBObjectProperty.SCHEMA_OBJECT, true);
        properties.set(DBObjectProperty.INVALIDABLE, true);
    }

    @Override
    protected void initLists(ConnectionHandler connection) {
        super.initLists(connection);
        DBDataset dataset = getDataset();
        if (dataset != null) {
            DBObjectListContainer childObjects = ensureChildObjects();
            childObjects.createSubcontentObjectList(DBObjectType.COLUMN, this, dataset, DBObjectRelationType.INDEX_COLUMN);
        }
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.INDEX;
    }

    @NotNull
    @Override
    public String getQualifiedName(boolean quoted) {
        return getSchemaName(quoted) + '.' + getName(quoted);
    }

    @Override
    public DBDataset getDataset() {
        return getParentObject();
    }

    @Override
    public List<DBColumn> getColumns() {
        return getChildObjects(DBObjectType.COLUMN);
    }

    @Override
    public boolean isUnique() {
        return is(DBObjectProperty.UNIQUE);
    }

    @Override
    protected @Nullable List<DBObjectNavigationList> createNavigationLists() {
        List<DBObjectNavigationList> navigationLists = new LinkedList<>();

        List<DBColumn> columns = getColumns();
        if (columns.size() > 0) {
            navigationLists.add(DBObjectNavigationList.create("Columns", columns));
        }
        navigationLists.add(DBObjectNavigationList.create("Dataset", getDataset()));

        return navigationLists;
    }

    @Override
    public void buildToolTip(HtmlToolTipBuilder ttb) {
        ttb.append(true, getObjectType().getName(), true);
        ttb.createEmptyRow();
        super.buildToolTip(ttb);
    }

    /********************************************************
     *                   TreeeElement                       *
     * ******************************************************/

    @Override
    public boolean isLeaf() {
        return true;
    }
}
