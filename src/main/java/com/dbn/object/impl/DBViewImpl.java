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
import com.dbn.database.common.metadata.def.DBViewMetadata;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.editor.DBContentType;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.object.DBSchema;
import com.dbn.object.DBType;
import com.dbn.object.DBView;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.object.common.list.DBObjectNavigationList;
import com.dbn.object.common.property.DBObjectProperty;
import com.dbn.object.filter.type.ObjectTypeFilterSettings;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import static com.dbn.common.Priority.HIGHEST;

class DBViewImpl extends DBDatasetImpl<DBViewMetadata> implements DBView {
    private DBObjectRef<DBType> type;
    DBViewImpl(DBSchema schema, DBViewMetadata metadata) throws SQLException {
        super(schema, metadata);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBViewMetadata metadata) throws SQLException {
        String name = metadata.getViewName();
        set(DBObjectProperty.SYSTEM_OBJECT, metadata.isSystemView());
        String typeOwner = metadata.getViewTypeOwner();
        String typeName = metadata.getViewType();
        if (typeOwner != null && typeName != null) {
            DBObjectBundle objectBundle = connection.getObjectBundle();
            DBSchema typeSchema = objectBundle.getSchema(typeOwner);
            type = DBObjectRef.of(typeSchema == null ? null : typeSchema.getType(typeName));
        }
        return name;
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.VIEW;
    }

    @Override
    public DBType getType() {
        return DBObjectRef.get(type);
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
                getChildObjectList(DBObjectType.DATASET_TRIGGER));
    }

    @Override
    public boolean hasVisibleTreeChildren() {
        ObjectTypeFilterSettings settings = getObjectTypeFilterSettings();
        return
            settings.isVisible(DBObjectType.COLUMN) ||
            settings.isVisible(DBObjectType.CONSTRAINT) ||
            settings.isVisible(DBObjectType.DATASET_TRIGGER);
    }

    @Override
    public boolean isEditable(DBContentType contentType) {
        return contentType == DBContentType.CODE;
    }

    @Override
    protected @Nullable List<DBObjectNavigationList> createNavigationLists() {
        DBType type = getType();
        if (type != null) {
            List<DBObjectNavigationList> navigationLists = new LinkedList<>();
            navigationLists.add(DBObjectNavigationList.create("Type", type));
            return navigationLists;
        }
        return null;
    }

    @Override
    public boolean isSystemView() {
        return is(DBObjectProperty.SYSTEM_OBJECT);
    }

    /*********************************************************
     *                  DBEditableCodeObject                 *
     ********************************************************/

    @Override
    public void executeUpdateDDL(DBContentType contentType, String oldCode, String newCode) throws SQLException {

        DatabaseInterfaceInvoker.execute(HIGHEST,
                "Updating source code",
                "Updating sources of " + getQualifiedNameWithType(),
                getProject(),
                getConnectionId(),
                getSchemaId(),
                conn -> {
                    ConnectionHandler connection = getConnection();
                    DatabaseDataDefinitionInterface dataDefinition = connection.getDataDefinitionInterface();
                    dataDefinition.updateView(getName(true), newCode, conn);
                });
    }

    @Override
    public String getCodeParseRootId(DBContentType contentType) {
        return "subquery";
    }

    @Override
    public DBLanguage getCodeLanguage(DBContentType contentType) {
        return SQLLanguage.INSTANCE;
    }
}
