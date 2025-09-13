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
import com.dbn.assistant.tool.AssistantToolInfo.ToolDefinition;
import com.dbn.assistant.tool.AssistantToolInfo.UtilityDefinition;
import com.dbn.assistant.tool.impl.ProgramMetadataToolImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.List;

import static com.dbn.assistant.tool.AssistantToolCategory.METADATA_PROVIDER;
import static com.dbn.assistant.tool.AssistantToolInfo.FactoryDefinition;

@ToolDefinition(
    type = "PROGRAM_METADATA",
    name = "Program metadata",
    category = METADATA_PROVIDER,
    description = "Information about program units (functions, stored procedures, packages, declared types) in a given schema")
public interface ProgramMetadataTool extends AssistantTool {

    @FactoryDefinition(
        spec = ProgramMetadataTool.class,
        impl = ProgramMetadataToolImpl.class)
    class Factory extends AssistantToolFactoryBase<ProgramMetadataTool> {}

    /*********************************************
     *                 TOOLS                     *
     *********************************************/

    @Tool(
        name = "LIST_TYPE_NAMES",
        value = {
            "type=PROGRAM_METADATA",
            "category=METADATA_PROVIDER",
            "Lists the names of declared data-types in a given schema"})
    @UtilityDefinition(
        name = "List declared type names",
        summary = "schema %s")
    List<String> listTypeNames(@P("Schema name") String schemaName);


    @Tool(
        name = "LIST_FUNCTION_NAMES",
        value = {
                "type=PROGRAM_METADATA",
                "category=METADATA_PROVIDER",
                "Lists the names of functions in a given schema"})
    @UtilityDefinition(
        name = "List function names",
        summary = "schema %s")
    List<String> listFunctionNames(@P("Schema name") String schemaName);

    @Tool(
        name = "LIST_PROCEDURE_NAMES",
        value = {
                "type=PROGRAM_METADATA",
                "category=METADATA_PROVIDER",
                "Lists the names of stored procedures in a given schema"})
    @UtilityDefinition(
        name = "List procedure names",
        summary = "schema %s")
    List<String> listProcedureNames(@P("Schema name") String schemaName);

    @Tool(
        name = "LIST_PACKAGE_NAMES",
        value = {
                "type=PROGRAM_METADATA",
                "category=METADATA_PROVIDER",
                "Lists the names of packages in a given schema"})
    @UtilityDefinition(
        name = "List package names",
        summary = "schema %s")
    List<String> listPackageNames(@P("Schema name") String schemaName);

}
