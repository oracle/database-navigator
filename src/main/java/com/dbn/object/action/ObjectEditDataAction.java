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
import com.dbn.editor.DatabaseFileEditorManager;
import com.dbn.editor.EditorProviderId;
import com.dbn.object.common.DBObject;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.exception.Exceptions.unsupported;
import static com.dbn.editor.DBContentType.DATA;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.common.property.DBObjectProperty.EDITABLE;

public class ObjectEditDataAction extends AnObjectAction<DBObject> {
    public ObjectEditDataAction(DBObject object) {
        super(object);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable DBObject object) {
        if (!isSupported(object)) {
            presentation.setVisible(false);
            return;
        }

        presentation.setText(txt("app.objects.action.EditData"));
        presentation.setIcon(Icons.OBJECT_EDIT_DATA);
        presentation.setVisible(true);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull DBObject object) {
        if (!isSupported(object)) unsupported(DATA);

        DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(project);
        editorManager.connectAndOpenEditor(object, EditorProviderId.DATA, false, true);
    }

    public static boolean isSupported(@Nullable DBObject object) {
        if (isNotValid(object)) return false;
        if (!object.is(EDITABLE)) return false;

        return object.getContentType().has(DATA);
    }
}
