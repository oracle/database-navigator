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

package com.dbn.language.editor.action;

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.Lookups;
import com.dbn.common.action.SelectDropdownAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.dbn.ddl.DDLFileAttachmentManager;
import com.dbn.object.DBSchema;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.vfs.DatabaseFileSystem;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.List;

import static com.dbn.common.action.Lookups.getProject;
import static com.dbn.common.action.Lookups.getVirtualFile;
import static com.dbn.connection.ConnectionHandler.isLiveConnection;
import static com.dbn.nls.NlsResources.txt;
import static java.util.Collections.emptyList;

@BackgroundUpdate
public class SchemaSelectDropdownAction extends SelectDropdownAction<DBSchema> implements DumbAware {

    public SchemaSelectDropdownAction() {
        super(txt("app.codeEditor.action.ScriptEditorCurrentSchema"));
    }

    @Override
    protected List<DBSchema> getObjects(DataContext dataContext) {
        Project project = getProject(dataContext);
        if (project == null) return null;

        VirtualFile virtualFile = getVirtualFile(dataContext);
        if (virtualFile == null) return null;

        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
        ConnectionHandler connection = contextManager.getConnection(virtualFile);
        if (connection == null) return emptyList();

        return connection.getObjectBundle().getSchemas();
    }

    @Override
    protected DBSchema getSelectedObject(AnActionEvent e) {
        Project project = getProject(e);
        if (project == null) return null;

        VirtualFile virtualFile = getVirtualFile(e);
        if (virtualFile == null) return null;

        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
        ConnectionHandler connection = contextManager.getConnection(virtualFile);
        if (connection == null) return null;

        SchemaId schemaId = contextManager.getDatabaseSchema(virtualFile);
        return connection.getSchema(schemaId);
    }

    @Override
    protected void setSelectedObject(AnActionEvent e, DBSchema object) {
        Project project = getProject(e);
        if (project == null) return;

        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
        SchemaId schemaId = SchemaId.from(object);

        Editor editor = Lookups.getEditor(e);
        if (editor != null) {
            contextManager.setDatabaseSchema(editor, schemaId);
            return;
        }

        FileEditor fileEditor = Lookups.getFileEditor(e);
        if (fileEditor != null) {
            VirtualFile file = fileEditor.getFile();
            contextManager.setDatabaseSchema(file, schemaId);
        }
    }

    @Override
    protected boolean isEnabled(AnActionEvent e) {
        Project project = getProject(e);
        if (project == null) return true;

        VirtualFile virtualFile = getVirtualFile(e);
        if (virtualFile == null) return true;
        if (!virtualFile.isInLocalFileSystem()) return true;

        DDLFileAttachmentManager fileAttachmentManager = DDLFileAttachmentManager.getInstance(project);
        DBObjectRef editableObject = fileAttachmentManager.getMappedObjectRef(virtualFile);
        if (editableObject == null) return true;

        return !DatabaseFileSystem.isFileOpened(editableObject);
    }

    @Override
    protected boolean isVisible(AnActionEvent e) {
        Project project = getProject(e);
        if (project == null) return true;

        VirtualFile virtualFile = getVirtualFile(e);
        if (virtualFile == null) return true;

        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
        ConnectionHandler connection = contextManager.getConnection(virtualFile);
        return isLiveConnection(connection);
    }

    @Override
    protected String getDescription(AnActionEvent e) {
        return txt("app.codeEditor.tooltip.SelectCurrentSchema");
    }
 }
