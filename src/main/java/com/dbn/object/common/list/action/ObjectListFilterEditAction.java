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
import com.dbn.connection.ConnectionId;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.filter.ObjectFilterManager;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.nls.NlsResources.txt;

public class ObjectListFilterEditAction extends BasicAction {

    private final ConnectionId connectionId;
    private final DBObjectType objectType;

    public ObjectListFilterEditAction(DBObjectList objectList) {
        super(txt("app.objects.action.EditFilter"));
        this.connectionId = objectList.getConnectionId();
        this.objectType = objectList.getObjectType();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (isNotValid(project)) return;

        ObjectFilterManager filterManager = ObjectFilterManager.getInstance(project);
        filterManager.openObjectFilterDialog(connectionId, objectType);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (isNotValid(project)) return;

        ObjectFilterManager filterManager = ObjectFilterManager.getInstance(project);
        boolean filterAvailable = filterManager.hasObjectFilter(connectionId, objectType);
        boolean global = filterManager.isQuickFilterFeatureActive();

        String text = filterAvailable ?
                txt(global ? "app.objects.action.EditGlobalFilter" : "app.objects.action.EditFilterDialog") :
                txt(global ? "app.objects.action.CreateGlobalFilter" : "app.objects.action.CreateFilter");

        e.getPresentation().setText(text);
    }
}
