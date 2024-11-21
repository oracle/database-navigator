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
import com.dbn.database.common.metadata.def.DBNestedTableMetadata;
import com.dbn.object.DBNestedTable;
import com.dbn.object.DBNestedTableColumn;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.DBType;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectImpl;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

class DBNestedTableImpl extends DBObjectImpl<DBNestedTableMetadata> implements DBNestedTable {
    private List<DBNestedTableColumn> columns;
    private DBObjectRef<DBType> typeRef;

    DBNestedTableImpl(DBTable parent, DBNestedTableMetadata metadata) throws SQLException {
        super(parent, metadata);

    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBNestedTableMetadata metadata) throws SQLException {
        String name = metadata.getNestedTableName();

        String typeOwner = metadata.getDeclaredTypeOwner();
        String typeName = metadata.getDeclaredTypeName();
        DBSchema schema = connection.getObjectBundle().getSchema(typeOwner);
        typeRef = DBObjectRef.of(schema == null ? null : schema.getType(typeName));
        // todo !!!
        return name;
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.NESTED_TABLE;
    }

    @Override
    public List<DBNestedTableColumn> getColumns() {
        if (columns == null) {
            columns = new ArrayList<>();
            //todo
        }
        return columns;
    }

    @Override
    public DBNestedTableColumn getColumn(String name) {
        return getChildObject(DBObjectType.COLUMN, name);
    }

    @Override
    public DBTable getTable() {
        return getParentObject();
    }

    public DBType getType() {
        return DBObjectRef.get(typeRef);
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
    public boolean isLeaf() {
        return true;
    }

    @Override
    @NotNull
    public List<BrowserTreeNode> buildPossibleTreeChildren() {
        return EMPTY_TREE_NODE_LIST;
        //return getColumns();
    }

    @Override
    public boolean hasVisibleTreeChildren() {
        return false;
        //ObjectTypeFilterSettings settings = getConnection().getSettings().getFilterSettings().getObjectTypeFilterSettings();
        //return settings.isVisible(DBObjectType.COLUMN);
    }
}
