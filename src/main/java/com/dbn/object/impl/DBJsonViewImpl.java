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

package com.dbn.object.impl;

import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.common.util.Lists;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.def.DBJsonViewMetadata;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBJsonView;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.list.DBObjectListContainer;
import com.dbn.object.common.list.DBObjectNavigationList;
import com.dbn.object.common.property.DBObjectProperty;
import com.dbn.object.common.status.DBObjectStatus;
import com.dbn.object.common.status.DBObjectStatusHolder;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.dbn.common.Priority.HIGHEST;
import static com.dbn.object.type.DBObjectRelationType.JSON_VIEW_TABLE;
import static com.dbn.object.type.DBObjectType.TABLE;

@Getter
class DBJsonViewImpl extends DBViewImpl<DBJsonViewMetadata> implements DBJsonView {
    private String jsonSchema;
    private String jsonColumnName;
    private DBObjectRef<DBTable> rootTable;
    private List<String> keyAttributeNames;

    DBJsonViewImpl(DBSchema schema, DBJsonViewMetadata metadata) throws SQLException {
        super(schema, metadata);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBJsonViewMetadata metadata) throws SQLException {
        String name = super.initObject(connection, parentObject, metadata);
        set(DBObjectProperty.READONLY, metadata.isReadonly());
        set(DBObjectProperty.INSERT_ALLOWED, metadata.isInsertAllowed());
        set(DBObjectProperty.UPDATE_ALLOWED, metadata.isUpdateAllowed());
        set(DBObjectProperty.DELETE_ALLOWED, metadata.isDeleteAllowed());

        set(DBObjectProperty.INVALIDABLE, true);

        jsonSchema = metadata.getJsonSchema();
        jsonColumnName = metadata.getJsonColumnName();

        DBObjectRef<DBSchema> rootTableSchema = new DBObjectRef<>(connection.getConnectionId(), DBObjectType.SCHEMA, metadata.getRootTableOwner());
        rootTable = new DBObjectRef<>(rootTableSchema, DBObjectType.TABLE, metadata.getRootTableName());

        keyAttributeNames = Lists.fromCsv(metadata.getKeyAttributeNames());

        return name;
    }

    @Override
    public void initStatus(DBJsonViewMetadata metadata) throws SQLException {
        boolean isValid = metadata.isValid();
        DBObjectStatusHolder objectStatus = getStatus();
        objectStatus.set(DBObjectStatus.VALID, isValid);
    }

    @Override
    protected void initLists(ConnectionHandler connection) {
        DBSchema schema = getSchema();
        DBObjectListContainer childObjects = ensureChildObjects();
        //childObjects.createSubcontentObjectRelationList(JSON_VIEW_TABLE, this, schema);
        childObjects.createSubcontentObjectList(TABLE, this, schema, JSON_VIEW_TABLE);
    }

    @Override
    public boolean isEditable(DBContentType contentType) {
        if (contentType == DBContentType.CODE) return true;
        if (contentType == DBContentType.JSON) return !isReadonly();

        return false;
    }

    @Override
    public boolean isReadonly() {
        return is(DBObjectProperty.READONLY);
    }

    @Override
    public boolean isInsertAllowed() {
        return is(DBObjectProperty.INSERT_ALLOWED);
    }

    @Override
    public boolean isUpdateAllowed() {
        return is(DBObjectProperty.UPDATE_ALLOWED);
    }

    @Override
    public boolean isDeleteAllowed() {
        return is(DBObjectProperty.DELETE_ALLOWED);
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.JSON_VIEW;
    }

    public List<DBTable> getTables() {
        return getChildObjects(TABLE);
    }

    @Nullable
    public DBTable getRootTable() {
        return DBObjectRef.get(rootTable);
    }

    @Override
    protected @Nullable List<DBObjectNavigationList> createNavigationLists() {
        List<DBObjectNavigationList> navigationLists = new LinkedList<>();

        List<DBTable> tables = getTables();
        navigationLists.add(DBObjectNavigationList.create("Tables", tables));

        DBTable rootTable = getRootTable();
        if (rootTable != null) {
            navigationLists.add(DBObjectNavigationList.create("Root Table", rootTable));
            return navigationLists;
        }

        return null;
    }

    /*********************************************************
     *                  DBEditableCodeObject                 *
     ********************************************************/

    @Override
    public void executeUpdateDDL(DBContentType contentType, String oldCode, String newCode) throws SQLException {
        DatabaseInterfaceInvoker.execute(HIGHEST,
                "Updating source code",
                "Updating source of " + getQualifiedNameWithType(),
                getProject(),
                getConnectionId(),
                getSchemaId(),
                conn -> {
                    ConnectionHandler connection = getConnection();
                    DatabaseDataDefinitionInterface dataDefinition = connection.getDataDefinitionInterface();
                    dataDefinition.updateJsonView(
                            getSchemaName(true),
                            getName(true),
                            newCode,
                            isEditionable(),
                            conn);
                });
    }

    @Override
    public String getCodeParseRootId(DBContentType contentType) {
        return "select_json_statement";
    }

    /*********************************************************
     *                     TreeElement                       *
     *********************************************************/
    @Override
    @NotNull
    public List<BrowserTreeNode> buildPossibleTreeChildren() {
        return Collections.emptyList();
    }

    @Override
    public boolean hasVisibleTreeChildren() {
        return false;
    }

}
