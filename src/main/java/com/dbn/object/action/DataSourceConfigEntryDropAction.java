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

package com.dbn.object.action;

import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectionAction;
import com.dbn.object.DBDataSourceConfigEntry;
import com.dbn.object.management.ObjectManagementService;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.util.Conditional.when;
import static com.dbn.common.util.Messages.OPTIONS_YES_NO;
import static com.dbn.common.util.Messages.showQuestionDialog;
import static com.dbn.nls.NlsResources.txt;

public class DataSourceConfigEntryDropAction extends AnObjectAction<DBDataSourceConfigEntry> {
    public DataSourceConfigEntryDropAction(@NotNull DBDataSourceConfigEntry object) {
        super(object);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable DBDataSourceConfigEntry object) {
        presentation.setText(txt("app.objects.action.Drop"));
        presentation.setIcon(Icons.ACTION_CLOSE);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull DBDataSourceConfigEntry object) {
        showQuestionDialog(
                project,
                txt("msg.objects.title.DropObject"),
                txt("msg.objects.question.DropObject", object.getQualifiedNameWithType()),
                OPTIONS_YES_NO, 0,
                option -> when(option == 0, () ->
                        ConnectionAction.invoke(txt("msg.objects.title.DroppingObject"), false, object, action -> {
                            ObjectManagementService objectManagementService = ObjectManagementService.getInstance(project);
                            objectManagementService.deleteObject(object, null);
                        })));
    }
}
