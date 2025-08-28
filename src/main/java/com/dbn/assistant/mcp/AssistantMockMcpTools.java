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

package com.dbn.assistant.mcp;

import dev.langchain4j.agent.tool.Tool;

public class AssistantMockMcpTools {
    @Tool("List all tables in the database")
    public String listTables() {
        return executeTool("listTables", "{}");
    }

    @Tool("Get details of a specific table in the database")
    public String getTableDetails(String tableName) {
        return executeTool("getTableDetails", "{ \"table\": \"" + tableName + "\" }");
    }

    @Tool("Execute a SQL query against the database")
    public String executeSql(String sql) {
        return executeTool("executeSql", "{ \"sql\": \"" + sql + "\" }");
    }

    private String executeTool(String listTables, String arguments) {
        return new AssistantMockMcpClient().call(listTables, arguments);
    }
}
