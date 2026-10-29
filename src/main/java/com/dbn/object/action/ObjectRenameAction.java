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
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.management.DatabaseObjectManager;
import com.dbn.object.management.ObjectManagementService;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.common.property.DBObjectProperty.READONLY;
import static com.dbn.object.common.property.DBObjectProperty.RENAMABLE;
import static com.dbn.object.common.property.DBObjectProperty.SYSTEM_OBJECT;
import static com.dbn.object.event.ObjectChangeAction.RENAME;

public class ObjectRenameAction extends AnObjectAction<DBObject> {
    public ObjectRenameAction(DBObject object) {
        super(object);
    }

    @Override
    protected void actionPerformed(
            @NotNull AnActionEvent e,
            @NotNull Project project,
            @NotNull DBObject object) {

        ObjectManagementService objectManagementService = ObjectManagementService.getInstance(project);
        if (!isSupported(object) || !objectManagementService.supports(object)) {
            throw new UnsupportedOperationException();
        }
        DatabaseObjectManager.getInstance(project).renameObject(object);
    }

    @Override
    protected void update(
            @NotNull AnActionEvent e,
            @NotNull Presentation presentation,
            @NotNull Project project,
            @Nullable DBObject target) {

        if (!isSupported(target)) {
            presentation.setVisible(false);
            return;
        }

        presentation.setText(txt("app.objects.action.Rename"));
        presentation.setIcon(Icons.ACTION_EDIT);
        presentation.setVisible(true);
    }

    public static boolean isSupported(@Nullable DBObject object) {
        if (isNotValid(object)) return false;
        if (!object.is(RENAMABLE)) return false;
        if (object.is(READONLY)) return false;
        if (object.is(SYSTEM_OBJECT)) return false;

        ObjectManagementService managementService = ObjectManagementService.getInstance(object.getProject());
        if (!managementService.supports(object)) return false;

        DBSchema schema = object.getSchema();
        if (schema == null) return false;
        if (schema.isSystemSchema()) return false;
        if (schema.isPublicSchema()) return false;

        return object.getCompatibilityInterface().supportsObjectAction(object.getObjectType().getTypeId(), RENAME);
    }
}
