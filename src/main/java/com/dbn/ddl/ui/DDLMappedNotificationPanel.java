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

package com.dbn.ddl.ui;

import com.dbn.common.editor.EditorNotificationPanel;
import com.dbn.common.message.MessageType;
import com.dbn.ddl.DDLFileAttachmentManager;
import com.dbn.editor.DatabaseFileEditorManager;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class DDLMappedNotificationPanel extends EditorNotificationPanel {
    private final DBObjectRef<DBSchemaObject> object;

    public DDLMappedNotificationPanel(@NotNull Project project, @NotNull VirtualFile file, DBSchemaObject object) {
        super(project, file, MessageType.WARNING);
        this.object = DBObjectRef.of(object);

        String objectName = object.getQualifiedNameWithType();
        String objectTypeName = object.getObjectType().getName();
        setText("This DDL file is attached to the database " + objectName + ". " +
                "Changes done to the " + objectTypeName + " are mirrored to this DDL file, overwriting any changes you may do to it.");

        createActionLabel("Detach", () -> detach());
    }

    private void detach() {
        Project project = getProject();
        VirtualFile file = getFile();

        DDLFileAttachmentManager attachmentManager = DDLFileAttachmentManager.getInstance(project);
        attachmentManager.detachDDLFile(file);
        DBSchemaObject object = DBObjectRef.get(this.object);
        if (object == null) return;

        DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(project);
        editorManager.reopenEditor(object);
    }
}
