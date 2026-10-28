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

package com.dbn.object.management;

import com.dbn.common.attributes.Attributes;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Dialogs;
import com.dbn.object.DBColumn;
import com.dbn.object.DBConstraint;
import com.dbn.object.DBIndex;
import com.dbn.object.common.DBObject;
import com.dbn.object.management.ui.ObjectRenameDialog;
import com.dbn.vfs.DatabaseFileManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.util.Messages.showInfoDialog;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.management.ObjectManagementRequest.NEW_NAME;

/** Collects object management input before invoking the service layer. */
public class DatabaseObjectManager extends ProjectComponentBase {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseObjectManager";

    private DatabaseObjectManager(Project project) {
        super(project, COMPONENT_NAME);
    }

    public static DatabaseObjectManager getInstance(@NotNull Project project) {
        return projectService(project, DatabaseObjectManager.class);
    }

    public void renameObject(@NotNull DBObject object) {
        Dispatch.run(true, () -> {
            DBObject editorObject = object instanceof DBColumn column ? column.getDataset() :
                    object instanceof DBConstraint constraint ? constraint.getDataset() :
                    object instanceof DBIndex index ? index.getDataset() : object;
            if (DatabaseFileManager.getInstance(getProject()).isFileOpened(editorObject)) {
                showInfoDialog(getProject(), txt("msg.objects.title.RenameObject"),
                        txt("msg.objects.info.CloseEditorBeforeRename", editorObject.getQualifiedNameWithType()));
                return;
            }

            Dialogs.show(() -> new ObjectRenameDialog(object), (dialog, exitCode) -> {
                if (exitCode != DialogWrapper.OK_EXIT_CODE) return;
                Attributes attributes = Attributes.create().set(NEW_NAME, dialog.getNewName());
                ObjectManagementService.getInstance(getProject()).renameObject(object, attributes, null);
            });
        });
    }
}
