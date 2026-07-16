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
import com.dbn.object.common.DBObject;
import com.dbn.object.editor.ObjectEditorProviders;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

/**
 * Generic edit action for any managed object whose {@link DBObject#getObjectType()} has a registered
 * {@link com.dbn.object.editor.ObjectEditorProvider}. The provider owns the type-specific input dialog.
 */
public class ObjectEditAction extends AnObjectAction<DBObject> {
    public ObjectEditAction(@NotNull DBObject object) {
        super(object);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable DBObject object) {
        presentation.setText(txt("app.objects.action.EditObject"));
        presentation.setIcon(Icons.ACTION_EDIT);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull DBObject object) {
        ObjectEditorProviders.get(object.getObjectType()).openEditDialog(object);
    }
}
