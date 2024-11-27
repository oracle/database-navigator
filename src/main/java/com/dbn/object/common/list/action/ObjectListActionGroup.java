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

package com.dbn.object.common.list.action;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.DatabaseEntity;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.actionSystem.DefaultActionGroup;

public class ObjectListActionGroup extends DefaultActionGroup {

    public ObjectListActionGroup(DBObjectList objectList) {
        add(new ReloadObjectsAction(objectList));
        DatabaseEntity parentElement = objectList.getParentEntity();
        ConnectionHandler connection = objectList.getConnection();
        if(parentElement instanceof DBSchema) {
            add (new ObjectListFilterAction(objectList));
            addSeparator();
            add (new CreateObjectAction(objectList));
        } else if (parentElement instanceof DBObjectBundle) {
            add (new ObjectListFilterAction(objectList));
            DBObjectType objectType = objectList.getObjectType();
            if (objectType == DBObjectType.SCHEMA) {
                add (new HideEmptySchemasToggleAction(connection));
            }
        } else if (objectList.getObjectType() == DBObjectType.COLUMN) {
            add(new HidePseudoColumnsToggleAction(connection));
            add(new HideAuditColumnsToggleAction(connection));
        }
    }
}