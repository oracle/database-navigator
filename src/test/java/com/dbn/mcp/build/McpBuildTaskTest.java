/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.mcp.build;

import com.dbn.mcp.model.McpServerDefinition;
import com.dbn.mcp.model.McpToolDefinition;
import com.dbn.mcp.model.McpToolParam;
import com.dbn.mcp.model.McpToolParamType;
import com.dbn.mcp.model.McpTransportType;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class McpBuildTaskTest {

    @Test
    public void buildYamlRedactsCredentials() {
        McpServerDefinition definition = new McpServerDefinition();
        definition.setTransportType(McpTransportType.STDIO);
        definition.setHttpPort("8080");
        definition.setTools(List.of(createTool()));

        String yaml = McpBuildTask.buildYaml(
                definition,
                "jdbc:oracle:thin:scott/tiger@localhost:1521/orclpdb?password=Secret123&token=ApiToken");

        assertTrue(yaml.contains("url: \"jdbc:oracle:thin:redacted_user/redacted_password@localhost:1521/orclpdb?password=redacted_password&token=redacted_token\""));
        assertTrue(yaml.contains("# username: YOUR_USER"));
        assertTrue(yaml.contains("# password: YOUR_PASS"));
        assertFalse(yaml.contains("scott"));
        assertFalse(yaml.contains("tiger"));
        assertFalse(yaml.contains("Secret123"));
        assertFalse(yaml.contains("ApiToken"));
        assertFalse(yaml.contains("\n  username:"));
        assertFalse(yaml.contains("\n  password:"));
    }

    private static McpToolDefinition createTool() {
        McpToolDefinition tool = new McpToolDefinition();
        tool.setName("find_employee");
        tool.setDescription("Find employee by id");
        tool.setStatement("select * from employees where employee_id = :employee_id");
        tool.setParameters(List.of(new McpToolParam(
                ":employee_id",
                McpToolParamType.INTEGER,
                "101",
                "Employee id",
                true)));
        return tool;
    }
}
