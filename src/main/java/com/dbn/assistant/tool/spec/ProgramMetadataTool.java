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
import com.dbn.assistant.tool.impl.ProgramMetadataToolImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.List;

import static com.dbn.assistant.tool.AssistantToolCategory.METADATA_PROVIDER;
import static com.dbn.assistant.tool.AssistantToolInfo.FactorySpec;
import static com.dbn.assistant.tool.AssistantToolType.PROGRAM_METADATA;

@ToolSpec(
        category = METADATA_PROVIDER,
        type = PROGRAM_METADATA,
        name = "Program metadata",
        description = "Information about program units (functions, stored procedures, packages, declared types) in a given schema")
public interface ProgramMetadataTool extends AssistantTool {

    @FactorySpec(
        spec = ProgramMetadataTool.class,
        impl = ProgramMetadataToolImpl.class)
    class Factory extends AssistantToolFactoryBase<ProgramMetadataTool> {}

    /*********************************************
     *                 TOOLS                     *
     *********************************************/

    @Tool(name = "LIST_PROGRAM_NAMES")
    @UtilitySpec(
            name = "List program names",
            description = "Lists the names of programs of a given type in a schema",
            summary = "schema %s - %s")
    List<String> listProgramNames(
            @P("Schema name") String schemaName,
            @P("Program type (FUNCTION, PROCEDURE, PACKAGE or TYPE)") String programType,
            @P(value = "Optional name filter (see REGEX_NAME_EXPRESSION tool instruction)", required = false) String programNameRegex);


    @Tool(name = "LIST_TYPE_NAMES")
    @UtilitySpec(
            name = "List declared type names",
            description = "Lists the names of declared data-types in a given schema",
            summary = "schema %s",
            discontinued = true) // token optimization (replaced by generic LIST_PROGRAM_NAMES)
    List<String> listTypeNames(
            @P("Schema name") String schemaName,
            @P(value = "Optional name filter (see REGEX_NAME_EXPRESSION tool instruction)", required = false) String typeNameRegex);


    @Tool(name = "LIST_FUNCTION_NAMES")
    @UtilitySpec(
            name = "List function names",
            description = "Lists the names of functions in a given schema",
            summary = "schema %s",
            discontinued = true) // token optimization (replaced by generic LIST_PROGRAM_NAMES)
    List<String> listFunctionNames(
            @P("Schema name") String schemaName,
            @P(value = "Optional name filter (see REGEX_NAME_EXPRESSION tool instruction)", required = false) String functionNameRegex);


    @Tool(name = "LIST_PROCEDURE_NAMES")
    @UtilitySpec(
            name = "List procedure names",
            description = "Lists the names of stored procedures in a given schema",
            summary = "schema %s",
            discontinued = true) // token optimization (replaced by generic LIST_PROGRAM_NAMES)
    List<String> listProcedureNames(
            @P("Schema name") String schemaName,
            @P(value = "Optional name filter (see REGEX_NAME_EXPRESSION tool instruction)", required = false) String procedureNameRegex);


    @Tool(name = "LIST_PACKAGE_NAMES")
    @UtilitySpec(
            name = "List package names",
            description = "Lists the names of packages in a given schema",
            summary = "schema %s",
            discontinued = true) // token optimization (replaced by generic LIST_PROGRAM_NAMES)
    List<String> listPackageNames(
            @P("Schema name") String schemaName,
            @P(value = "Optional name filter (see REGEX_NAME_EXPRESSION tool instruction)", required = false) String packageNameRegex);

}
