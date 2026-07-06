/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.object.diagram.actions;

import com.dbn.object.action.AnObjectAction;
import com.dbn.object.common.DBObject;
import com.dbn.object.diagram.DatabaseDiagramManager;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

public final class OpenDependencyDiagramAction extends AnObjectAction<DBObject> {
    public OpenDependencyDiagramAction(DBObject object) {
        super(object);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent event, @NotNull Project project, @NotNull DBObject object) {
        DatabaseDiagramManager.getInstance(project).showDiagram(object, event);
    }

    @Override
    protected void update(@NotNull AnActionEvent event, @NotNull Presentation presentation,
                          @NotNull Project project, @Nullable DBObject object) {
        if (object != null) {
            presentation.setText(txt("app.browser.action.DependencyDiagram"));
            presentation.setIcon(AllIcons.FileTypes.Diagram);
        }
    }
}
