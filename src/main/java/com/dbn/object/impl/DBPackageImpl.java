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
import com.dbn.browser.ui.HtmlToolTipBuilder;
import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.def.DBPackageMetadata;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBPackage;
import com.dbn.object.DBPackageFunction;
import com.dbn.object.DBPackageProcedure;
import com.dbn.object.DBPackageType;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.list.DBObjectListContainer;
import com.dbn.object.common.status.DBObjectStatus;
import com.dbn.object.filter.type.ObjectTypeFilterSettings;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.SQLException;
import java.util.List;

import static com.dbn.object.type.DBObjectType.FUNCTION;
import static com.dbn.object.type.DBObjectType.PACKAGE;
import static com.dbn.object.type.DBObjectType.PACKAGE_FUNCTION;
import static com.dbn.object.type.DBObjectType.PACKAGE_PROCEDURE;
import static com.dbn.object.type.DBObjectType.PACKAGE_TYPE;
import static com.dbn.object.type.DBObjectType.PROCEDURE;
import static com.dbn.object.type.DBObjectType.TYPE;

class DBPackageImpl
        extends DBProgramImpl<DBPackageMetadata, DBPackageProcedure, DBPackageFunction, DBPackageType>
        implements DBPackage {

    DBPackageImpl(DBSchema schema, DBPackageMetadata metadata) throws SQLException {
        super(schema, metadata);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBPackageMetadata metadata) throws SQLException {
        return metadata.getPackageName();
    }

    @Override
    protected void initLists(ConnectionHandler connection) {
        super.initLists(connection);
        DBSchema schema = getSchema();
        DBObjectListContainer childObjects = ensureChildObjects();
        childObjects.createSubcontentObjectList(PACKAGE_FUNCTION, this, schema);
        childObjects.createSubcontentObjectList(PACKAGE_PROCEDURE, this, schema);
        childObjects.createSubcontentObjectList(PACKAGE_TYPE, this, schema);
    }

    @Override
    protected DBObjectType getFunctionObjectType() {
        return PACKAGE_FUNCTION;
    }

    @Override
    protected DBObjectType getProcedureObjectType() {
        return PACKAGE_PROCEDURE;
    }

    @Override
    protected DBObjectType getTypeObjectType() {
        return PACKAGE_TYPE;
    }

    @Override
    public List<DBPackageType> getTypes() {
        return getChildObjects(PACKAGE_TYPE);
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return PACKAGE;
    }

    @Override
    @Nullable
    public Icon getIcon() {
        if (getStatus().is(DBObjectStatus.VALID)) {
            if (getStatus().is(DBObjectStatus.DEBUG))  {
                return Icons.DBO_PACKAGE_DEBUG;
            } else {
                return Icons.DBO_PACKAGE;
            }
        } else {
            return Icons.DBO_PACKAGE_ERR;
        }
    }

    @Override
    public Icon getOriginalIcon() {
        return Icons.DBO_PACKAGE;
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
        return DatabaseBrowserUtils.createList(
                getChildObjectList(PACKAGE_PROCEDURE),
                getChildObjectList(PACKAGE_FUNCTION),
                getChildObjectList(PACKAGE_TYPE));
    }

    @Override
    public boolean hasVisibleTreeChildren() {
        ObjectTypeFilterSettings settings = getObjectTypeFilterSettings();
        return
            settings.isVisible(PROCEDURE) ||
            settings.isVisible(FUNCTION) ||
            settings.isVisible(TYPE);
    }

    @Override
    public String getCodeParseRootId(DBContentType contentType) {
        return contentType == DBContentType.CODE_SPEC ? "package_spec" :
               contentType == DBContentType.CODE_BODY ? "package_body" : null;
    }
}
