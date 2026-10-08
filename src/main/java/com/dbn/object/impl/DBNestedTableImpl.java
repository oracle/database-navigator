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

import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.browser.ui.HtmlToolTipBuilder;
import com.dbn.connection.ConnectionHandler;
import com.dbn.data.type.DBDataType;
import com.dbn.database.common.metadata.def.DBNestedTableMetadata;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBColumn;
import com.dbn.object.DBConstraint;
import com.dbn.object.DBDatasetTrigger;
import com.dbn.object.DBNestedTable;
import com.dbn.object.DBNestedTableColumn;
import com.dbn.object.DBIndex;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObjectImpl;
import com.dbn.object.common.list.DBObjectListContainer;
import com.dbn.object.filter.type.ObjectTypeFilterSettings;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static com.dbn.browser.DatabaseBrowserUtils.createList;
import static com.dbn.object.type.DBObjectType.COLUMN;
import static com.dbn.object.type.DBObjectType.NESTED_TABLE_COLUMN;

class DBNestedTableImpl extends DBSchemaObjectImpl<DBNestedTableMetadata> implements DBNestedTable {
    private DBObjectRef<DBColumn> parentTableColumn;

    DBNestedTableImpl(DBTable parent, DBNestedTableMetadata metadata) throws SQLException {
        super(parent, metadata);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBNestedTableMetadata metadata) throws SQLException {
        parentTableColumn = new DBObjectRef<>(parentObject.ref(), COLUMN, metadata.getParentTableColumnName());
        return metadata.getNestedTableName();
    }

    @Override
    protected void initLists(ConnectionHandler connection) {
        super.initLists(connection);
        DBSchema schema = getSchema();
        DBObjectListContainer childObjects = ensureChildObjects();
        childObjects.createSubcontentObjectList(NESTED_TABLE_COLUMN, this, schema);
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.NESTED_TABLE;
    }

    @Override
    @NotNull
    public List<DBColumn> getColumns() {
        return getChildObjects(NESTED_TABLE_COLUMN);
    }

    @Override
    public DBNestedTableColumn getColumn(String name) {
        return getChildObject(NESTED_TABLE_COLUMN, name);
    }

    @Override
    @Nullable
    public List<DBConstraint> getConstraints() {
        return Collections.emptyList();
    }

    @Override
    @Nullable
    public DBConstraint getConstraint(String name) {
        return null;
    }

    @Override
    @Nullable
    public List<DBDatasetTrigger> getTriggers() {
        return Collections.emptyList();
    }

    @Override
    @Nullable
    public DBDatasetTrigger getTrigger(String name) {
        return null;
    }

    @Override
    @Nullable
    public List<DBIndex> getIndexes() {
        return Collections.emptyList();
    }

    @Override
    @Nullable
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
    public boolean isEditable(DBContentType contentType) {
        return false;
    }

    @Override
    public DBTable getParentTable() {
        return getParentObject();
    }

    @Override
    public DBColumn getParentTableColumn() {
        return DBObjectRef.get(parentTableColumn);
    }

    @Override
    public void buildToolTip(HtmlToolTipBuilder ttb) {
        ttb.append(true, getObjectType().getName(), true);
        ttb.createEmptyRow();
        super.buildToolTip(ttb);
    }

    /*********************************************************
     *                     TreeElement                       *
     *********************************************************/

    @Override
    @NotNull
    public List<BrowserTreeNode> buildPossibleTreeChildren() {
        return createList(getChildObjectList(NESTED_TABLE_COLUMN));
    }

    @Override
    public boolean hasVisibleTreeChildren() {
        ObjectTypeFilterSettings settings = getObjectTypeFilterSettings();
        return settings.isVisible(NESTED_TABLE_COLUMN);
    }
}
