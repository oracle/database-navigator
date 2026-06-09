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

import com.dbn.common.action.DefaultActionGroup;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

public class ObjectListFilterActionGroup extends DefaultActionGroup {

    public ObjectListFilterActionGroup(DBObjectList objectList) {
        ConnectionHandler connection = objectList.getConnection();

        DBObjectType objectType = objectList.getObjectType();
        add(new ObjectListQuickFilterAction(objectList));
        addSeparator();
        add(new ObjectListFilterEditAction(objectList));
        add(new ObjectListFilterToggleAction(objectList));
        add(new ObjectListFilterRemoveAction(objectList));
        addSeparator();

        add(new HideObjectTypeAction(objectList));
        if (objectType == DBObjectType.SCHEMA) {
            add (new HideEmptySchemasToggleAction(connection));
        } else if (objectType == DBObjectType.COLUMN) {
            add(new HidePseudoColumnsToggleAction(connection));
            add(new HideAuditColumnsToggleAction(connection));
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setText(txt("app.objects.action.Filters"));
        e.getPresentation().setPopupGroup(true);
    }
}
