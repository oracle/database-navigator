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
import com.dbn.object.filter.custom.ObjectFilter;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.nls.NlsResources.txt;

public class ObjectListFilterToggleAction extends BasicAction {

    private final ConnectionId connectionId;
    private final DBObjectType objectType;

    public ObjectListFilterToggleAction(DBObjectList objectList) {
        super(txt("app.objects.action.ToggleFilter"));
        this.connectionId = objectList.getConnectionId();
        this.objectType = objectList.getObjectType();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (isNotValid(project)) return;

        ObjectFilterManager filterManager = ObjectFilterManager.getInstance(project);
        filterManager.toggleFilter(connectionId, objectType);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (isNotValid(project)) return;

        Presentation presentation = e.getPresentation();
        ObjectFilterManager filterManager = ObjectFilterManager.getInstance(project);
        ObjectFilter objectFilter = filterManager.getObjectFilter(connectionId, objectType);
        if (objectFilter == null) {
            presentation.setVisible(false);
            return;
        }

        boolean global = filterManager.isQuickFilterFeatureActive();
        String text = objectFilter.isActive() ?
                txt(global ? "app.objects.action.DisableGlobalFilter" : "app.objects.action.DisableFilter") :
                txt(global ? "app.objects.action.EnableGlobalFilter" : "app.objects.action.EnableFilter");

        presentation.setText(text);
    }
}
