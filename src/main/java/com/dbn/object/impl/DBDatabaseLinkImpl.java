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
import com.dbn.database.common.metadata.def.DBDatabaseLinkMetadata;
import com.dbn.object.DBDatabaseLink;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObjectImpl;
import com.dbn.object.common.property.DBObjectProperty;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

@Getter
class DBDatabaseLinkImpl extends DBSchemaObjectImpl<DBDatabaseLinkMetadata> implements DBDatabaseLink {
    private String userName;
    private String host;

    DBDatabaseLinkImpl(DBSchema schema, DBDatabaseLinkMetadata metadata) throws SQLException {
        super(schema, metadata);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBDatabaseLinkMetadata metadata) throws SQLException {
        String name = metadata.getDblinkName();
        userName = metadata.getUserName();
        host = metadata.getHost();
        return name;
    }

    @Override
    public void initProperties() {
        properties.set(DBObjectProperty.SCHEMA_OBJECT, true);
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.DBLINK;
    }

    @Override
    public void buildToolTip(HtmlToolTipBuilder ttb) {
        ttb.append(true, getObjectType().getName(), true);
        ttb.append(true, host, false);
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

}
