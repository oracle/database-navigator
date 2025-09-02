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
import com.dbn.assistant.tool.AssistantTool.Definition;
import com.dbn.assistant.tool.AssistantToolFactoryBase;
import com.dbn.assistant.tool.impl.TableMetadataToolImpl;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.assistant.tool.AssistantToolCategory.METADATA_PROVIDER;

@Description("Provides information about the tables in a given database schema")
@Definition(type = "TABLE_METADATA", category = METADATA_PROVIDER, impl = TableMetadataToolImpl.class)
public interface TableMetadataTool extends AssistantTool {

    class Factory extends AssistantToolFactoryBase<TableMetadataTool> {
        public Factory() {
            super(TableMetadataTool.class);
        }
    }

    /*********************************************
     *                 TOOLS                     *
     *********************************************/

    @Tool("List table names in a given schema")
    List<String> listTableNames(String schemaName, boolean includeTemporaryTables);

    @Tool("Loads the definition of a given table")
    TableDefinition loadTableDefinition(String schemaName, String tableName);

    @Tool("Loads the definitions of a given set of tables")
    List<TableDefinition> loadTableDefinitions(String schemaName, List<String> tableNames);

    @Data
    class TableDefinition {
        private String name;
        private List<ColumnDefinition> columns = new ArrayList<>();
    }

    @Data
    class ColumnDefinition {
        private String name;
        private String type;
        private String description;
    }
}
