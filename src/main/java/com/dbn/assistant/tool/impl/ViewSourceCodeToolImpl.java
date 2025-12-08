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
import com.dbn.assistant.tool.spec.ViewSourceCodeTool;
import com.dbn.common.util.Commons;
import com.dbn.editor.DBContentType;
import com.dbn.editor.code.SourceCodeManager;
import com.dbn.editor.code.content.SourceCodeContent;
import com.dbn.object.DBSchema;
import com.dbn.object.DBView;
import com.dbn.object.type.DBObjectType;

import java.sql.SQLException;

public class ViewSourceCodeToolImpl extends AssistantToolBase implements ViewSourceCodeTool {
    @Override
    public ViewSourceCode loadViewSourceCode(String schemaName, String viewName) throws SQLException {
        DBSchema schema = getSchema(schemaName);

        DBView view = Commons.coalesce(
                () -> schema.getView(viewName),
                () -> schema.getMaterializedView(viewName),
                () -> schema.getJsonView(viewName));
        verify(view, DBObjectType.VIEW, viewName);

        SourceCodeManager sourceCodeManager = SourceCodeManager.getInstance(view.getProject());
        SourceCodeContent sourceCode = sourceCodeManager.loadSourceFromDatabase(view, DBContentType.CODE);

        ViewSourceCode viewSourceCode = new ViewSourceCode();
        viewSourceCode.setName(viewName);
        viewSourceCode.setCode(sourceCode.getRawContent());
        return viewSourceCode;
    }
}
