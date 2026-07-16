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

import com.dbn.assistant.mcp.model.AssistantMcpServer;
import com.dbn.assistant.mcp.model.AssistantMcpServerData;
import com.dbn.assistant.mcp.model.AssistantMcpToolInfo;
import com.dbn.common.EntityId;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import org.junit.Test;

import static com.dbn.assistant.mcp.model.AssistantMcpToolMetadataGuard.MAX_TOOL_DESCRIPTION_LENGTH;
import static com.dbn.assistant.mcp.model.AssistantMcpToolMetadataGuard.MAX_TOOL_INSTRUCTION_LENGTH;
import static com.dbn.assistant.mcp.model.AssistantMcpToolMetadataGuard.MAX_TOOL_NAME_LENGTH;
import static com.dbn.assistant.mcp.model.AssistantMcpToolMetadataGuard.isAcceptedToolMetadata;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AssistantMcpServerDataTest {

    @Test
    public void createToolInfoHandlesNullDescription() {
        AssistantMcpToolInfo toolInfo = AssistantMcpServerData.createToolInfo(
                createServer(),
                createSpecification("usr_mcp0_test_tool", null, ""));

        assertEquals("test_tool", toolInfo.getName());
        assertEquals("", toolInfo.getDescription());
        assertEquals("", toolInfo.getInstruction());
        assertNotNull(toolInfo.getSignature());
    }

    @Test
    public void createToolInfoTruncatesRenderedDescription() {
        String paragraph = "a".repeat(MAX_TOOL_DESCRIPTION_LENGTH + 100);
        AssistantMcpToolInfo toolInfo = AssistantMcpServerData.createToolInfo(
                createServer(),
                createSpecification("usr_mcp0_test_tool", paragraph + "\n\nSecond paragraph", ""));

        assertEquals(MAX_TOOL_DESCRIPTION_LENGTH, toolInfo.getDescription().length());
        assertTrue(toolInfo.isTruncated());
    }

    @Test
    public void acceptsMetadataWithinLimits() {
        ToolSpecification specification = createSpecification(
                "tool",
                "a".repeat(MAX_TOOL_INSTRUCTION_LENGTH),
                "parameter");

        assertTrue(isAcceptedToolMetadata(specification));
    }

    @Test
    public void rejectsOversizedToolName() {
        ToolSpecification specification = createSpecification(
                "a".repeat(MAX_TOOL_NAME_LENGTH + 1),
                "description",
                "");

        assertFalse(isAcceptedToolMetadata(specification));
    }

    @Test
    public void rejectsOversizedDescription() {
        ToolSpecification specification = createSpecification(
                "tool",
                "a".repeat(MAX_TOOL_INSTRUCTION_LENGTH + 1),
                "");

        assertFalse(isAcceptedToolMetadata(specification));
    }

    @Test
    public void rejectsOversizedParameterSchema() {
        ToolSpecification specification = createSpecification(
                "tool",
                "description",
                "a".repeat(MAX_TOOL_INSTRUCTION_LENGTH + 1));

        assertFalse(isAcceptedToolMetadata(specification));
    }

    private static AssistantMcpServer createServer() {
        AssistantMcpServer server = new AssistantMcpServer(EntityId.get("test-mcp-server"));
        server.setKey("usr_mcp0");
        return server;
    }

    private static ToolSpecification createSpecification(String name, String description, String parameterDescription) {
        JsonObjectSchema parameters = JsonObjectSchema.builder()
                .description(parameterDescription)
                .build();

        return ToolSpecification.builder()
                .name(name)
                .description(description)
                .parameters(parameters)
                .build();
    }
}
