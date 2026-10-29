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

package com.dbn.object.action;

import com.dbn.common.icon.Icons;
import com.dbn.object.DBColumn;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.factory.DatabaseObjectFactory;
import com.dbn.object.management.ObjectManagementService;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.exception.Exceptions.unsupported;
import static com.dbn.database.DatabaseFeature.CONSTRAINT_MANIPULATION;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.common.property.DBObjectProperty.READONLY;
import static com.dbn.object.common.property.DBObjectProperty.SCHEMA_OBJECT;
import static com.dbn.object.common.property.DBObjectProperty.SYSTEM_OBJECT;
import static com.dbn.object.event.ObjectChangeAction.DELETE;
import static com.dbn.object.type.DBObjectType.CONSTRAINT;

public class ObjectDropAction extends AnObjectAction<DBObject> {
    public ObjectDropAction(DBObject object) {
        super(object);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable DBObject object) {
        if (!isSupported(object)) {
            presentation.setVisible(false);
            return;
        }

        presentation.setText(txt("app.objects.action.Drop"));
        presentation.setIcon(Icons.ACTION_CLOSE);
        presentation.setVisible(true);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull DBObject object) {
        if (!isSupported(object)) unsupported(DELETE);

        DatabaseObjectFactory objectFactory = DatabaseObjectFactory.getInstance(project);
        objectFactory.dropObject(object);
    }

    public static boolean isSupported(@Nullable DBObject object) {
        if (isNotValid(object)) return false;
        if (object.is(READONLY)) return false;
        if (object.is(SYSTEM_OBJECT)) return false;
        if (object instanceof DBColumn) return false;

        if (object instanceof DBSchemaObject) {
            DBSchema schema = object.getSchema();
            if (!object.is(SCHEMA_OBJECT)) return false;
            if (schema.isSystemSchema()) return false;
            if (schema.isPublicSchema()) return false;

            return object.getObjectType() != CONSTRAINT || CONSTRAINT_MANIPULATION.isSupported(object);
        }

        ObjectManagementService managementService = ObjectManagementService.getInstance(object.getProject());
        return managementService.supports(object);
    }
}
