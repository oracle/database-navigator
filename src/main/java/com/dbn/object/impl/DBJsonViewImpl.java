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
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.def.DBJsonViewMetadata;
import com.dbn.object.DBJsonView;
import com.dbn.object.DBSchema;
import com.dbn.object.common.list.DBObjectListContainer;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

class DBJsonViewImpl extends DBViewImpl implements DBJsonView {
    DBJsonViewImpl(DBSchema schema, DBJsonViewMetadata metadata) throws SQLException {
        super(schema, metadata);
    }

    @Override
    protected void initLists(ConnectionHandler connection) {
        DBSchema schema = getSchema();
        DBObjectListContainer childObjects = ensureChildObjects();
        //childObjects.createSubcontentObjectList(DBObjectType.INDEX, this, schema);
        //childObjects.createSubcontentObjectRelationList(DBObjectRelationType.INDEX_COLUMN, this, schema);
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.JSON_VIEW;
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
