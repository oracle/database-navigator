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

package com.dbn.assistant.tool.spec;

import com.dbn.assistant.tool.AssistantTool;
import com.dbn.assistant.tool.AssistantToolFactoryBase;
import com.dbn.assistant.tool.AssistantToolInfo.ToolSpec;
import com.dbn.assistant.tool.AssistantToolInfo.UtilitySpec;
import com.dbn.assistant.tool.impl.DatasetEditorToolImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import static com.dbn.assistant.tool.AssistantToolCategory.IDE_ACTION_INVOKER;
import static com.dbn.assistant.tool.AssistantToolInfo.FactorySpec;
import static com.dbn.assistant.tool.AssistantToolType.DATASET_EDITORS;

@ToolSpec(
        category = IDE_ACTION_INVOKER,
        type = DATASET_EDITORS,
        name = "Dataset editors",
        description = "IDE actions for viewing and editing datasets (such as tables, views, materialized views)")
public interface DatasetEditorTool extends AssistantTool {

    @FactorySpec(
            spec = DatasetEditorTool.class,
            impl = DatasetEditorToolImpl.class)
    class Factory extends AssistantToolFactoryBase<DatasetEditorTool> {}

    /*********************************************
     *                 TOOLS                     *
     *********************************************/

    @Tool(name = "OPEN_TABLE_EDITOR")
    @UtilitySpec(
            name = "Open table data editor",
            description = "Opens the editor of the specified table in the IDE",
            summary = "%s.%s")
    void openTableDataEditor(
            @P("Schema name") String schemaName,
            @P("Table name") String tableName);


    @Tool(name = "OPEN_VIEW_DATA_EDITOR")
    @UtilitySpec(
            name = "Open view data editor",
            description = "Opens the data editor of the specified view in the IDE",
            summary = "%s.%s")
    void openViewDataEditor(
            @P("Schema name") String schemaName,
            @P("View name") String viewName);
}
