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
import com.dbn.assistant.tool.impl.SourceCodeEditorToolImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.sql.SQLException;

import static com.dbn.assistant.tool.AssistantToolCategory.IDE_ACTION_INVOKER;
import static com.dbn.assistant.tool.AssistantToolInfo.FactorySpec;
import static com.dbn.assistant.tool.AssistantToolType.SOURCE_CODE_EDITORS;

@ToolSpec(
        category = IDE_ACTION_INVOKER,
        type = SOURCE_CODE_EDITORS,
        name = "Source-code editors",
        description = "IDE actions for editing source-code of database objects")
public interface SourceCodeEditorTool extends AssistantTool {

    @FactorySpec(
            spec = SourceCodeEditorTool.class,
            impl = SourceCodeEditorToolImpl.class)
    class Factory extends AssistantToolFactoryBase<SourceCodeEditorTool> {}

    /*********************************************
     *                 TOOLS                     *
     *********************************************/

    @Tool(name = "OPEN_PROGRAM_CODE_EDITOR")
    @UtilitySpec(
            name = "Open program code editor",
            description = "Opens the code editor of a given program in the IDE",
            summary = "%s.%s - %s")
    void openProgramCodeEditor(
            @P("Schema name") String schemaName,
            @P("Program name") String programName,
            @P("Program type (FUNCTION, PROCEDURE, PACKAGE or TYPE)") String programType) throws SQLException;


    @Tool(name = "OPEN_TYPE_CODE_EDITOR")
    @UtilitySpec(
            name = "Open type code editor",
            description = "Opens the code editor of the specified user-defined type in the IDE",
            summary = "%s.%s",
            discontinued = true) // token optimization (replaced by generic OPEN_PROGRAM_CODE_EDITOR)
    void openTypeCodeEditor(
            @P("Schema name") String schemaName,
            @P("Type name") String typeName);


    @Tool(name = "OPEN_PACKAGE_CODE_EDITOR")
    @UtilitySpec(
            name = "Open package code editor",
            description = "Opens the code editor of the specified package in the IDE",
            summary = "%s.%s",
            discontinued = true) // token optimization (replaced by generic OPEN_PROGRAM_CODE_EDITOR)
    void openPackageEditor(
            @P("Schema name") String schemaName,
            @P("Package name") String packageName);


    @Tool(name = "OPEN_FUNCTION_CODE_EDITOR")
    @UtilitySpec(
            name = "Open function code editor",
            description = "Opens the editor of the specified function in the IDE",
            summary = "%s.%s",
            discontinued = true) // token optimization (replaced by generic OPEN_PROGRAM_CODE_EDITOR)
    void openFunctionCodeEditor(
            @P("Schema name") String schemaName,
            @P("Function name") String functionName);


    @Tool(name = "OPEN_PROCEDURE_CODE_EDITOR")
    @UtilitySpec(
            name = "Open procedure code editor",
            description = "Opens the editor of the specified procedure in the IDE",
            summary = "%s.%s",
            discontinued = true) // token optimization (replaced by generic OPEN_PROGRAM_CODE_EDITOR)
    void openProcedureCodeEditor(
            @P("Schema name") String schemaName,
            @P("Procedure name") String procedureName);


    @Tool(name = "OPEN_VIEW_CODE_EDITOR")
    @UtilitySpec(
            name = "Open view code editor",
            description = "Opens the code editor of the specified view in the IDE",
            summary = "%s.%s")
    void openViewCodeEditor(
            @P("Schema name") String schemaName,
            @P("View name") String viewName);
}
