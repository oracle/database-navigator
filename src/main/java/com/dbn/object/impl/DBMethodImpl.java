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
import com.dbn.database.common.metadata.def.DBMethodMetadata;
import com.dbn.editor.DBContentType;
import com.dbn.language.common.DBLanguage;
import com.dbn.object.DBArgument;
import com.dbn.object.DBMethod;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.DBSchemaObjectImpl;
import com.dbn.object.common.list.DBObjectListContainer;
import com.dbn.object.common.status.DBObjectStatus;
import com.dbn.object.common.status.DBObjectStatusHolder;
import com.dbn.object.filter.type.ObjectTypeFilterSettings;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.List;

import static com.dbn.object.common.property.DBObjectProperty.COMPILABLE;
import static com.dbn.object.common.property.DBObjectProperty.DEBUGABLE;
import static com.dbn.object.common.property.DBObjectProperty.DETERMINISTIC;
import static com.dbn.object.common.property.DBObjectProperty.INVALIDABLE;
import static com.dbn.object.type.DBObjectType.ARGUMENT;

@Getter
abstract class DBMethodImpl<M extends DBMethodMetadata> extends DBSchemaObjectImpl<M> implements DBMethod {
    protected short position;
    protected short overload;
    private DBLanguage language;

    DBMethodImpl(DBSchemaObject parent, M resultSet) throws SQLException {
        super(parent, resultSet);
    }

    DBMethodImpl(DBSchema schema, M resultSet) throws SQLException {
        super(schema, resultSet);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, M metadata) throws SQLException {
        set(DETERMINISTIC, metadata.isDeterministic());
        overload = metadata.getOverload();
        position = metadata.getPosition();
        language = DBLanguage.getLanguage(metadata.getLanguage());
        return null;
    }

    @Override
    public void initProperties() {
        super.initProperties();
        properties.set(COMPILABLE, true);
        properties.set(INVALIDABLE, true);
        properties.set(DEBUGABLE, true);
    }

    @Override
    public void initStatus(M metadata) throws SQLException {
        boolean isValid = metadata.isValid();
        boolean isDebug = metadata.isDebug();
        DBObjectStatusHolder objectStatus = getStatus();
        objectStatus.set(DBObjectStatus.VALID, isValid);
        objectStatus.set(DBObjectStatus.DEBUG, isDebug);
    }

    @Override
    protected void initLists(ConnectionHandler connection) {
        super.initLists(connection);
        DBObjectListContainer childObjects = ensureChildObjects();
        childObjects.createSubcontentObjectList(ARGUMENT, this, getSchema());
    }

    @Override
    public boolean isEditable(DBContentType contentType) {
        return getContentType() == DBContentType.CODE && contentType == DBContentType.CODE;
    }

    @Override
    public boolean isDeterministic() {
        return is(DETERMINISTIC);
    }

    @Override
    public boolean hasDeclaredArguments() {
        for (DBArgument argument : getArguments()) {
            if (argument.getDataType().isDeclared()) {
                return true;
            }
        }
        return false; 
    }

    @Override
    public List<DBArgument> getArguments() {
        return getChildObjects(ARGUMENT);
    }

    @Override
    public DBArgument getReturnArgument() {
        return null;
    }

    @Override
    public DBArgument getArgument(String name) {
        return getChildObject(ARGUMENT, name);
    }

    @Override
    public String getPresentableTextDetails() {
        return overload > 0 ? " #" + overload : "";
    }

    @Override
    public boolean isProgramMethod() {
        return false;
    }

    @Override
    public int compareTo(@NotNull Object o) {
        int result = super.compareTo(o);
        if (result == 0) {
            DBMethod method = (DBMethod) o;
            return overload - method.getOverload();
        }
        return result;
    }

    /*********************************************************
     *                     TreeElement                       *
     *********************************************************/
    @Override
    @NotNull
    public List<BrowserTreeNode> buildPossibleTreeChildren() {
        return DatabaseBrowserUtils.createList(getChildObjectList(ARGUMENT));
    }

    @Override
    public boolean hasVisibleTreeChildren() {
        ObjectTypeFilterSettings settings = getObjectTypeFilterSettings();
        return settings.isVisible(ARGUMENT);
    }
}
