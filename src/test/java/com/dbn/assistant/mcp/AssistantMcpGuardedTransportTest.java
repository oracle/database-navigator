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

package com.dbn.assistant.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import static com.dbn.assistant.mcp.model.AssistantMcpToolMetadataGuard.MAX_TOOL_COUNT;
import static com.dbn.assistant.mcp.model.AssistantMcpToolMetadataGuard.MAX_TOOL_INSTRUCTION_LENGTH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class AssistantMcpGuardedTransportTest {

    @Test
    public void capsToolArrayBeforeToolSpecificationMaterialization() {
        JsonNode response = responseWithTools();
        ArrayNode tools = (ArrayNode) response.get("result").get("tools");
        for (int i = 0; i < MAX_TOOL_COUNT + 100; i++) {
            tools.add(tool("tool_" + i, "description", "schema"));
        }

        JsonNode guardedResponse = AssistantMcpGuardedTransport.guardToolMetadata(response);
        ArrayNode guardedTools = (ArrayNode) guardedResponse.get("result").get("tools");

        assertEquals(MAX_TOOL_COUNT, guardedTools.size());
    }

    @Test
    public void skipsOversizedToolMetadataBeforeToolSpecificationMaterialization() {
        JsonNode response = responseWithTools();
        ArrayNode tools = (ArrayNode) response.get("result").get("tools");
        tools.add(tool("accepted", "description", "schema"));
        tools.add(tool("oversized_description", "a".repeat(MAX_TOOL_INSTRUCTION_LENGTH + 1), "schema"));
        tools.add(tool("oversized_schema", "description", "a".repeat(MAX_TOOL_INSTRUCTION_LENGTH + 1)));
        tools.add(JsonNodeFactory.instance.objectNode().put("name", "missing_schema"));

        JsonNode guardedResponse = AssistantMcpGuardedTransport.guardToolMetadata(response);
        ArrayNode guardedTools = (ArrayNode) guardedResponse.get("result").get("tools");

        assertEquals(1, guardedTools.size());
        assertEquals("accepted", guardedTools.get(0).get("name").asText());
    }

    @Test
    public void leavesMalformedResponseUntouched() {
        ObjectNode response = JsonNodeFactory.instance.objectNode();
        response.putObject("result");

        JsonNode guardedResponse = AssistantMcpGuardedTransport.guardToolMetadata(response);

        assertSame(response, guardedResponse);
    }

    private static ObjectNode responseWithTools() {
        ObjectNode response = JsonNodeFactory.instance.objectNode();
        response.put("jsonrpc", "2.0");
        response.put("id", 1);
        response.putObject("result").putArray("tools");
        return response;
    }

    private static ObjectNode tool(String name, String description, String schemaDescription) {
        ObjectNode tool = JsonNodeFactory.instance.objectNode();
        tool.put("name", name);
        tool.put("description", description);

        ObjectNode schema = tool.putObject("inputSchema");
        schema.put("type", "object");
        schema.put("description", schemaDescription);
        return tool;
    }
}
