/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.assistant.tool.impl;

import com.dbn.assistant.tool.AssistantToolBase;
import com.dbn.assistant.tool.spec.DatasetEditorTool;
import com.dbn.common.util.Commons;
import com.dbn.editor.DatabaseFileEditorManager;
import com.dbn.editor.EditorProviderId;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.DBView;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;

public class DatasetEditorToolImpl extends AssistantToolBase implements DatasetEditorTool {

    @Override
    public void openTableDataEditor(String schemaName, String tableName) {
        DBSchema schema = getSchema(schemaName);
        DBTable table = schema.getTable(tableName);

        verify(table, DBObjectType.TABLE, tableName);
        openEditor(table);
    }

    @Override
    public void openViewDataEditor(String schemaName, String viewName) {
        DBSchema schema = getSchema(schemaName);
        DBView view = Commons.coalesce(
                () -> schema.getView(viewName),
                () -> schema.getMaterializedView(viewName),
                () -> schema.getJsonView(viewName));

        verify(view, DBObjectType.VIEW, viewName);
        openEditor(view);
    }

    private static void openEditor(DBSchemaObject object) {
        Project project = object.getProject();
        DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(project);
        editorManager.connectAndOpenEditor(object, EditorProviderId.DATA, true, true);
    }
}
