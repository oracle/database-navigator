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
import com.dbn.assistant.tool.AssistantToolInfo.Definition;
import com.dbn.assistant.tool.impl.TableMetadataToolImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

import java.util.List;

import static com.dbn.assistant.tool.AssistantToolCategory.METADATA_PROVIDER;
import static com.dbn.assistant.tool.AssistantToolInfo.FactoryDefinition;

@Definition(
    type = "TABLE_METADATA",
    category = METADATA_PROVIDER,
    description = "Provides information about the tables in a given database schema")
public interface TableMetadataTool extends AssistantTool {

    @FactoryDefinition(
        spec = TableMetadataTool.class,
        impl = TableMetadataToolImpl.class)
    class Factory extends AssistantToolFactoryBase<TableMetadataTool> {}

    /*********************************************
     *                 TOOLS                     *
     *********************************************/

    @Tool(
        name = "LIST_TABLE_NAMES",
        value = {
            "type=TABLE_METADATA",
            "category=METADATA_PROVIDER",
            "Lists table names in a given schema"})
    List<String> listTableNames(
            @P("Schema name") String schemaName,
            @P("Include temporary tables") boolean includeTemporaryTables);


    @Tool(
        name = "LIST_TEMPORARY_TABLE_NAMES",
        value = {
                "type=TABLE_METADATA",
                "category=METADATA_PROVIDER",
                "Lists temporary table names in a given schema"})
    List<String> listTemporaryTableNames(
            @P("Schema name") String schemaName);


    @Tool(
        name = "LOAD_TABLE_DEFINITION",
        value = {
                "type=TABLE_METADATA",
                "category=METADATA_PROVIDER",
                "Loads the definition of a given table"})
    TableDefinition loadTableDefinition(
            @P("Schema name") String schemaName,
            @P("Table name") String tableName,
            @P("Include detailed constraint information (may be slow to respond)") boolean detailed);

    @Tool(
        name = "LOAD_TABLE_DEFINITIONS",
        value = {
                "type=TABLE_METADATA",
                "category=METADATA_PROVIDER",
                "Loads the definitions of a given set of tables"})
    List<TableDefinition> loadTableDefinitions(
            @P("Schema name") String schemaName,
            @P("Table names") List<String> tableNames,
            @P("Include detailed constraint information (may be slow to respond)") boolean detailed);


    @Data
    @Description("Table definition")
    class TableDefinition {
        @Description("Table name")
        private String name;

        @Description("Table description")
        private String description;

        @Description("Column definitions")
        private List<ColumnDefinition> columns;

        @Description("Constraint definitions")
        private List<ConstraintDefinition> constraints;
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

    @Data
    @Description("Constraint definition")
    class ConstraintDefinition {
        @Description("Constraint name")
        private String name;

        @Description("Constraint type")
        private String type;

        @Description("Constraint check condition")
        private String checkCondition;

        @Description("Constraint columns")
        private List<String> columns;

        @Description("Foreign key constraint")
        private ConstraintDefinition foreignKeyConstraint;
    }
}
