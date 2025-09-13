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
import com.dbn.assistant.tool.spec.ViewMetadataTool;
import com.dbn.common.util.Commons;
import com.dbn.editor.DBContentType;
import com.dbn.editor.code.SourceCodeManager;
import com.dbn.editor.code.content.SourceCodeContent;
import com.dbn.object.DBColumn;
import com.dbn.object.DBMaterializedView;
import com.dbn.object.DBSchema;
import com.dbn.object.DBView;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.dbn.common.util.Lists.convert;

public class ViewMetadataToolImpl extends AssistantToolBase implements ViewMetadataTool {

    @Override
    public List<String> listViewNames(String schemaName) {
        DBSchema schema = getSchema(schemaName);

        List<DBView> views = schema.getViews();
        return getObjectNames(views, false);
    }

    @Override
    public List<String> listMaterializedViewNames(String schemaName) {
        DBSchema schema = getSchema(schemaName);

        List<DBMaterializedView> views = schema.getMaterializedViews();
        return getObjectNames(views, false);
    }

    @Override
    public ViewDefinition loadViewDefinition(String schemaName, String viewName, boolean detailed) {
        DBSchema schema = getSchema(schemaName);
        return loadViewDefinition(schema, viewName, detailed);
    }

    @Override
    public List<ViewDefinition> loadViewDefinitions(String schemaName, List<String> viewNames, boolean detailed) {
        DBSchema schema = getSchema(schemaName);
        return convert(viewNames, n -> loadViewDefinition(schema, n, detailed));
    }

    private ViewDefinition loadViewDefinition(DBSchema schema, String viewName, boolean detailed) {
        DBView view = Commons.coalesce(
                () -> schema.getView(viewName),
                () -> schema.getMaterializedView(viewName));

        verify(view, DBObjectType.VIEW, viewName);

        ViewDefinition viewDef = createDefinition(view);
        viewDef.setColumns(convert(undisposed(view).getColumns(), c -> createDefinition(c)));

        if (detailed) {
            try {
                SourceCodeManager sourceCodeManager = SourceCodeManager.getInstance(view.getProject());
                SourceCodeContent sourceCode = sourceCodeManager.loadSourceFromDatabase(view, DBContentType.CODE);
                viewDef.setQuery(sourceCode.getRawContent());
            } catch (Exception e) {
                viewDef.setQuery("Error loading view definition: " + e.getMessage());
            }
        }

        return viewDef;
    }

    private static @NotNull ViewDefinition createDefinition(DBView view) {
        ViewDefinition viewDef = new ViewDefinition();
        viewDef.setName(view.getQualifiedName());
        viewDef.setDescription(view.getComments());
        return viewDef;
    }

    private static ColumnDefinition createDefinition(DBColumn column) {
        ColumnDefinition columnDef = new ColumnDefinition();
        columnDef.setName(column.getName());
        columnDef.setType(column.getDataType().getName());
        columnDef.setDescription(column.getComments());
        return columnDef;
    }
}
