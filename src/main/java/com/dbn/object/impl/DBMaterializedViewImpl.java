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

import com.dbn.browser.DatabaseBrowserUtils;
import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.def.DBMaterializedViewMetadata;
import com.dbn.object.DBIndex;
import com.dbn.object.DBMaterializedView;
import com.dbn.object.DBSchema;
import com.dbn.object.common.list.DBObjectListContainer;
import com.dbn.object.filter.type.ObjectTypeFilterSettings;
import com.dbn.object.type.DBObjectRelationType;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;

class DBMaterializedViewImpl extends DBViewImpl implements DBMaterializedView {
    DBMaterializedViewImpl(DBSchema schema, DBMaterializedViewMetadata metadata) throws SQLException {
        super(schema, metadata);
    }

    @Override
    protected void initLists(ConnectionHandler connection) {
        super.initLists(connection);
        DBSchema schema = getSchema();
        DBObjectListContainer childObjects = ensureChildObjects();
        childObjects.createSubcontentObjectList(DBObjectType.INDEX, this, schema);
        childObjects.createSubcontentObjectRelationList(DBObjectRelationType.INDEX_COLUMN, this, schema);
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.MATERIALIZED_VIEW;
    }

    @Override
    @Nullable
    public List<DBIndex> getIndexes() {
        return getChildObjects(DBObjectType.INDEX);
    }

    @Override
    @Nullable
    public DBIndex getIndex(String name) {
        return getChildObject(DBObjectType.INDEX, name);
    }

    /*********************************************************
     *                     TreeElement                       *
     *********************************************************/
    @Override
    @NotNull
    public List<BrowserTreeNode> buildPossibleTreeChildren() {
        return DatabaseBrowserUtils.createList(
                getChildObjectList(DBObjectType.COLUMN),
                getChildObjectList(DBObjectType.CONSTRAINT),
                getChildObjectList(DBObjectType.INDEX),
                getChildObjectList(DBObjectType.DATASET_TRIGGER));
    }

    @Override
    public boolean hasVisibleTreeChildren() {
        ObjectTypeFilterSettings settings = getObjectTypeFilterSettings();
        return
            settings.isVisible(DBObjectType.COLUMN) ||
            settings.isVisible(DBObjectType.CONSTRAINT) ||
            settings.isVisible(DBObjectType.INDEX) ||
            settings.isVisible(DBObjectType.DATASET_TRIGGER);
    }
}
