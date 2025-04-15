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

import com.dbn.browser.options.ObjectFilterChangeListener;
import com.dbn.common.action.ProjectAction;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.util.Conditional;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.filter.type.ObjectTypeFilterSettings;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class HideObjectTypeAction extends ProjectAction {

    private final DBObjectList objectList;

    HideObjectTypeAction(DBObjectList objectList) {
        this.objectList = objectList;
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setText("Hide " + objectList.getObjectType().getCapitalizedListName());
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        ConnectionHandler connection = objectList.getConnection();
        ObjectTypeFilterSettings settings = connection.getSettings().getFilterSettings().getObjectTypeFilterSettings();

        DBObjectType objectType = objectList.getObjectType();
        String listName = objectType.getCapitalizedListName();

        String title = "Hide " + listName;
        String message = "Are you sure you want to hide the " + listName + " for the \"" + connection.getName() + "\" connection? " +
                "(you can undo this by accessing the connection Filter settings)";
        Messages.showQuestionDialog(project, title, message,
                Messages.options("Hide " + listName, "Cancel"), 0, o ->
                Conditional.when(o == 0, () -> {
                    settings.hideObjectType(objectType);

                    ConnectionId connectionId = objectList.getConnectionId();
                    ProjectEvents.notify(project,
                            ObjectFilterChangeListener.TOPIC,
                            (listener) -> listener.typeFiltersChanged(connectionId));
                }));



    }
}
