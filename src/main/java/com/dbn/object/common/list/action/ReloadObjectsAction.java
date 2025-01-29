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

import com.dbn.common.action.ProjectAction;
import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Progress;
import com.dbn.connection.ConnectionAction;
import com.dbn.object.common.list.DBObjectList;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

public class ReloadObjectsAction extends ProjectAction {

    private final DBObjectList objectList;

    ReloadObjectsAction(DBObjectList objectList) {
        this.objectList = objectList;
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setText(objectList.isLoaded() ?
                txt("app.objects.action.Reload") :
                txt("app.objects.action.Load"));
        presentation.setIcon(Icons.ACTION_REFRESH);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        String listName = objectList.getCapitalizedName();

        String title =  objectList.isLoaded() ?
                txt("msg.objects.title.ReloadingObjects", listName) :
                txt("msg.objects.title.LoadingObjects", listName);
        ConnectionAction.invoke(
                title, true, objectList,
                action -> Progress.prompt(project, objectList, true,
                        txt("prc.objects.title.LoadingObjects"),
                        txt("prc.objects.text.ReloadingObjects", objectList.getContentDescription()),
                        progress -> {
                            objectList.getConnection().getMetaDataCache().reset();
                            objectList.reload();
                        }));
    }
}
