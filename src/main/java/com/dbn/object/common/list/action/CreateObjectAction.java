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

import com.dbn.common.action.BasicAction;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.ref.WeakRef;
import com.dbn.object.DBSchema;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.factory.DatabaseObjectFactory;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class CreateObjectAction extends BasicAction {

    private final WeakRef<DBObjectList> objectList;

    CreateObjectAction(DBObjectList objectList) {
        super("New " + objectList.getObjectType().getName() + "...");
        this.objectList = WeakRef.of(objectList);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DBObjectList objectList = getObjectList();
        DBSchema schema = objectList.ensureParentEntity();
        Project project = schema.getProject();
        DatabaseObjectFactory factory = DatabaseObjectFactory.getInstance(project);
        factory.openFactoryInputDialog(schema, objectList.getObjectType());
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        DBObjectType objectType = getObjectList().getObjectType();
        e.getPresentation().setVisible(objectType.isOneOf(DBObjectType.FUNCTION, DBObjectType.PROCEDURE));
    }

    @NotNull
    public DBObjectList getObjectList() {
        return Failsafe.nn(WeakRef.get(objectList));
    }
}