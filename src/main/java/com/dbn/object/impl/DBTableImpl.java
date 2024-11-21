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
import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.def.DBTableMetadata;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBColumn;
import com.dbn.object.DBIndex;
import com.dbn.object.DBNestedTable;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.list.DBObjectListContainer;
import com.dbn.object.common.list.DBObjectNavigationList;
import com.dbn.object.filter.type.ObjectTypeFilterSettings;
import com.dbn.object.properties.PresentableProperty;
import com.dbn.object.properties.SimplePresentableProperty;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.dbn.object.common.property.DBObjectProperty.TEMPORARY;
import static com.dbn.object.type.DBObjectRelationType.INDEX_COLUMN;
import static com.dbn.object.type.DBObjectType.COLUMN;
import static com.dbn.object.type.DBObjectType.CONSTRAINT;
import static com.dbn.object.type.DBObjectType.DATASET_TRIGGER;
import static com.dbn.object.type.DBObjectType.INDEX;
import static com.dbn.object.type.DBObjectType.NESTED_TABLE;
import static com.dbn.object.type.DBObjectType.TABLE;

class DBTableImpl extends DBDatasetImpl<DBTableMetadata> implements DBTable {
    private static final List<DBColumn> EMPTY_COLUMN_LIST = Collections.unmodifiableList(new ArrayList<>());

    DBTableImpl(DBSchema schema, DBTableMetadata metadata) throws SQLException {
        super(schema, metadata);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBTableMetadata metadata) throws SQLException {
        String name = metadata.getTableName();
        set(TEMPORARY, metadata.isTemporary());
        return name;
    }

    @Override
    protected void initLists(ConnectionHandler connection) {
        super.initLists(connection);
        DBSchema schema = getSchema();
        DBObjectListContainer childObjects = ensureChildObjects();

        childObjects.createSubcontentObjectList(INDEX, this, schema);
        childObjects.createSubcontentObjectList(NESTED_TABLE, this, schema);
        childObjects.createSubcontentObjectRelationList(INDEX_COLUMN, this, schema);
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return TABLE;
    }

    @Override
    @Nullable
    public Icon getIcon() {
        return isTemporary() ?
                Icons.DBO_TMP_TABLE :
                Icons.DBO_TABLE;
    }

    @Override
    public boolean isTemporary() {
        return is(TEMPORARY);
    }

    @Override
    @Nullable
    public List<DBIndex> getIndexes() {
        return getChildObjects(INDEX);
    }

    @Override
    public List<DBNestedTable> getNestedTables() {
        return getChildObjects(NESTED_TABLE);
    }

    @Override
    @Nullable
    public DBIndex getIndex(String name) {
        return getChildObject(INDEX, name);
    }

    @Override
    public DBNestedTable getNestedTable(String name) {
        return getChildObject(NESTED_TABLE, name);
    }

    @Override
    public List<DBColumn> getPrimaryKeyColumns() {
        List<DBColumn> columns = null;
        for (DBColumn column : getColumns()) {
            if (column.isPrimaryKey()) {
                if (columns == null) {
                    columns = new ArrayList<>();
                }
                columns.add(column);
            }
        }
        return columns == null ? EMPTY_COLUMN_LIST : columns ;
    }

    @Override
    public List<DBColumn> getForeignKeyColumns() {
        List<DBColumn> columns = null;
        for (DBColumn column : getColumns()) {
            if (column.isForeignKey()) {
                if (columns == null) {
                    columns = new ArrayList<>();
                }
                columns.add(column);
            }
        }
        return columns == null ? EMPTY_COLUMN_LIST : columns ;
    }

    @Override
    public List<DBColumn> getUniqueKeyColumns() {
        List<DBColumn> columns = null;
        for (DBColumn column : getColumns()) {
            if (column.isUniqueKey()) {
                if (columns == null) {
                    columns = new ArrayList<>();
                }
                columns.add(column);
            }
        }
        return columns == null ? EMPTY_COLUMN_LIST : columns ;
    }

    @Override
    public boolean isEditable(DBContentType contentType) {
        return contentType == DBContentType.DATA;
    }


    @Override
    protected @Nullable List<DBObjectNavigationList> createNavigationLists() {
        List<DBObjectNavigationList> navigationLists = new LinkedList<>();
        List<DBIndex> indexes = getChildObjects(INDEX);
        if (indexes != null && indexes.size() > 0) {
            navigationLists.add(DBObjectNavigationList.create("Indexes", indexes));
        }

        return navigationLists;
    }

    /*********************************************************
     *                     TreeElement                       *
     *********************************************************/
    @Override
    @NotNull
    public List<BrowserTreeNode> buildPossibleTreeChildren() {
        return DatabaseBrowserUtils.createList(
                getChildObjectList(COLUMN),
                getChildObjectList(CONSTRAINT),
                getChildObjectList(INDEX),
                getChildObjectList(DATASET_TRIGGER),
                getChildObjectList(NESTED_TABLE));
    }

    @Override
    public boolean hasVisibleTreeChildren() {
        ObjectTypeFilterSettings settings = getObjectTypeFilterSettings();
        return
            settings.isVisible(COLUMN) ||
            settings.isVisible(CONSTRAINT) ||
            settings.isVisible(INDEX) ||
            settings.isVisible(DATASET_TRIGGER) ||
            settings.isVisible(NESTED_TABLE);
    }

    @Override
    public List<PresentableProperty> getPresentableProperties() {
        List<PresentableProperty> properties = super.getPresentableProperties();
        if (isTemporary()) {
            properties.add(0, new SimplePresentableProperty("Attributes", "temporary"));
        }
        return properties;
    }
}
