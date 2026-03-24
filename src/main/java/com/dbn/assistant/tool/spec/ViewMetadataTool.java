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
import com.dbn.assistant.tool.impl.ViewMetadataToolImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

import java.util.List;

import static com.dbn.assistant.tool.AssistantToolCategory.METADATA_PROVIDER;
import static com.dbn.assistant.tool.AssistantToolInfo.FactorySpec;
import static com.dbn.assistant.tool.AssistantToolType.VIEW_METADATA;

@ToolSpec(
    category = METADATA_PROVIDER,
    type = VIEW_METADATA,
    name = "View metadata",
    description = "Information about views in a given schema")
public interface ViewMetadataTool extends AssistantTool {

    @FactorySpec(
        spec = ViewMetadataTool.class,
        impl = ViewMetadataToolImpl.class)
    class Factory extends AssistantToolFactoryBase<ViewMetadataTool> {}

    /*********************************************
     *                 TOOLS                     *
     *********************************************/

    @Tool(name = "LIST_VIEW_NAMES")
    @UtilitySpec(
            name = "List view names",
            description = "Lists view names in a given schema",
            summary = "schema %s")
    List<String> listViewNames(
            @P("Schema name") String schemaName);


    @Tool(name = "LIST_MATERIALIZED_VIEW_NAMES")
    @UtilitySpec(
            name = "List materialized view names",
            description = "Lists materialized view names in a given schema",
            summary = "schema %s")
    List<String> listMaterializedViewNames(
            @P("Schema name") String schemaName);


    @Tool(name = "LIST_JSON_VIEW_NAMES")
    @UtilitySpec(
            name = "List json view names",
            description = "Lists json duality view names in a given schema",
            summary = "schema %s")
    List<String> listJsonViewNames(
            @P("Schema name") String schemaName);


    @Tool(name = "LOAD_VIEW_DEFINITION")
    @UtilitySpec(
            name = "Load view definition",
            description = "Loads the definition of a given view",
            summary = "%s.%s")
    ViewDefinition loadViewDefinition(
            @P("Schema name") String schemaName,
            @P("View name") String viewName);


    @Tool(name = "LOAD_VIEW_DEFINITIONS")
    @UtilitySpec(
            name = "Load view definitions",
            description = "Loads the definitions of a given set of views (optimized version of LOAD_VIEW_DEFINITION for bulk queries)",
            summary = "%s.%s")
    List<ViewDefinition> loadViewDefinitions(
            @P("Schema name") String schemaName,
            @P("View names") List<String> viewNames);


    @Data
    @Description("View definition")
    class ViewDefinition {
        @Description("View name")
        private String name;

        @Description("View select statement")
        private String query;

        @Description("View description")
        private String description;

        @Description("Column definitions")
        private List<ColumnDefinition> columns;
    }

    @Data
    @Description("Column definition")
    class ColumnDefinition {
        @Description("Column name")
        private String name;

        @Description("Column data type")
        private String type;

        @Description("Column description")
        private String description;
    }
}
