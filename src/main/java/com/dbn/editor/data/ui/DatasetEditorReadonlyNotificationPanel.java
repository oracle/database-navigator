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

package com.dbn.editor.data.ui;

import com.dbn.common.environment.EnvironmentManager;
import com.dbn.common.message.MessageType;
import com.dbn.common.util.Messages;
import com.dbn.editor.DBContentType;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.options.ConfigId;
import com.dbn.options.ProjectSettingsManager;
import com.intellij.openapi.project.Project;

import static com.dbn.common.util.Conditional.when;
import static com.dbn.nls.NlsResources.txt;

public class DatasetEditorReadonlyNotificationPanel extends DatasetEditorNotificationPanel{
    public DatasetEditorReadonlyNotificationPanel(DBSchemaObject object) {
        super(object, isReadonly(object) ? MessageType.INFO : MessageType.WARNING);
        String environmentName = object.getEnvironmentType().getName();
        final Project project = object.getProject();

        if (isReadonly(object)) {
            setText(txt("ntf.dataEditor.text.ReadonlyData", environmentName));
            createActionLabel(txt("app.dataEditor.link.EditMode"),
                    () -> Messages.showQuestionDialog(project,
                            txt("msg.dataEditor.title.EnableEditMode"),
                            txt("msg.dataEditor.question.EnableEditMode", object.getQualifiedNameWithType()),
                            Messages.OPTIONS_YES_CANCEL, 0,
                            option -> when(option == 0, () -> {
                                EnvironmentManager environmentManager = EnvironmentManager.getInstance(project);
                                environmentManager.enableEditing(object, DBContentType.DATA);
                            })));
        } else {
            setText(txt("ntf.dataEditor.text.EditableData", environmentName));
            createActionLabel(txt("app.dataEditor.link.CancelEditing"), () -> {
                EnvironmentManager environmentManager = EnvironmentManager.getInstance(project);
                environmentManager.disableEditing(object, DBContentType.DATA);
            });
        }

        createActionLabel(txt("app.dataEditor.link.Settings"), () -> {
            ProjectSettingsManager settingsManager = ProjectSettingsManager.getInstance(project);
            settingsManager.openProjectSettings(ConfigId.GENERAL);
        });
    }

    private static boolean isReadonly(DBSchemaObject schemaObject) {
        Project project = schemaObject.getProject();
        EnvironmentManager environmentManager = EnvironmentManager.getInstance(project);
        return environmentManager.isReadonly(schemaObject, DBContentType.DATA);
    }
}
