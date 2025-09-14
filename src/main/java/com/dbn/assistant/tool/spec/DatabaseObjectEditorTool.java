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
import com.dbn.assistant.tool.impl.DatabaseObjectEditorToolImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import static com.dbn.assistant.tool.AssistantToolCategory.IDE_ACTION_INVOKER;
import static com.dbn.assistant.tool.AssistantToolInfo.FactorySpec;

@ToolSpec(
        category = IDE_ACTION_INVOKER,
        type = "DATABASE_OBJECT_EDITORS",
        name = "Database object editors",
        description = "IDE actions for editing database objects")
public interface DatabaseObjectEditorTool extends AssistantTool {

    @FactorySpec(
            spec = DatabaseObjectEditorTool.class,
            impl = DatabaseObjectEditorToolImpl.class)
    class Factory extends AssistantToolFactoryBase<DatabaseObjectEditorTool> {}

    /*********************************************
     *                 TOOLS                     *
     *********************************************/

    @Tool(name = "OPEN_TYPE_EDITOR")
    @UtilitySpec(
            name = "Open type editor",
            description = "Opens the editor of the specified user-defined type in the IDE",
            summary = "%s.%s")
    void openTypeEditor(
            @P("Schema name") String schemaName,
            @P("Type name") String typeName);


    @Tool(name = "OPEN_PACKAGE_EDITOR")
    @UtilitySpec(
            name = "Open package editor",
            description = "Opens the editor of the specified package in the IDE",
            summary = "%s.%s")
    void openPackageEditor(
            @P("Schema name") String schemaName,
            @P("Package name") String packageName);


    @Tool(name = "OPEN_FUNCTION_EDITOR")
    @UtilitySpec(
            name = "Open function editor",
            description = "Opens the editor of the specified function in the IDE",
            summary = "%s.%s")
    void openFunctionEditor(
            @P("Schema name") String schemaName,
            @P("Function name") String functionName);


    @Tool(name = "OPEN_PROCEDURE_EDITOR")
    @UtilitySpec(
            name = "Open procedure editor",
            description = "Opens the editor of the specified procedure in the IDE",
            summary = "%s.%s")
    void openProcedureEditor(
            @P("Schema name") String schemaName,
            @P("Procedure name") String procedureName);
}
