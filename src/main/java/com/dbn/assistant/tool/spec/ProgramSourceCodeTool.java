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
import com.dbn.assistant.tool.impl.ProgramSourceCodeToolImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

import java.sql.SQLException;

import static com.dbn.assistant.tool.AssistantToolCategory.SOURCE_CODE_PROVIDER;
import static com.dbn.assistant.tool.AssistantToolInfo.FactorySpec;

@ToolSpec(
    type = "PROGRAM_SOURCE_CODE",
    name = "Program source-code",
    category = SOURCE_CODE_PROVIDER,
    description = "Source code for program units (stored procedures, functions, packages, triggers, declared types)")
public interface ProgramSourceCodeTool extends AssistantTool {

    @FactorySpec(
        spec = ProgramSourceCodeTool.class,
        impl = ProgramSourceCodeToolImpl.class)
    class Factory extends AssistantToolFactoryBase<ProgramSourceCodeTool> {}

    /*********************************************
     *                 TOOLS                     *
     *********************************************/

    @Tool(name = "LOAD_TYPE_SOURCE_CODE")
    @UtilitySpec(
            name = "Load type source-code",
            description = "Loads the source code of a given user-defined type",
            summary = "%s.%s")
    ProgramSourceCode loadTypeSourceCode(
            @P("Schema name") String schemaName,
            @P("Type name") String typeName) throws SQLException;


    @Tool(name = "LOAD_PACKAGE_SOURCE_CODE")
    @UtilitySpec(
            name = "Load package source-code",
            description = "Loads the source code of a given package",
            summary = "%s.%s")
    ProgramSourceCode loadPackageSourceCode(
            @P("Schema name") String schemaName,
            @P("Package name") String packageName) throws SQLException;


    @Tool(name = "LOAD_FUNCTION_SOURCE_CODE")
    @UtilitySpec(
            name = "Load function source-code",
            description = "Loads the source code of a given function",
            summary = "%s.%s")
    MethodSourceCode loadFunctionSourceCode(
            @P("Schema name") String schemaName,
            @P("Function name") String functionName) throws SQLException;


    @Tool(name = "LOAD_PROCEDURE_SOURCE_CODE")
    @UtilitySpec(
            name = "Load procedure source-code",
            description = "Loads the source code of a given stored procedure",
            summary = "%s.%s")
    MethodSourceCode loadProcedureSourceCode(
            @P("Schema name") String schemaName,
            @P("Procedure name") String procedureName) throws SQLException;



    @Data
    @Description("Method source code")
    class MethodSourceCode {
        @Description("Method name")
        private String name;

        @Description("Code content")
        private String code;
    }

    @Data
    @Description("Program source code")
    class ProgramSourceCode {
        @Description("Program name")
        private String name;

        @Description("Program spec code")
        private String spec;

        @Description("Program body code")
        private String body;
    }
}
