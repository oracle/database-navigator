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

package com.dbn.assistant.mcp.model;

import com.dbn.common.checksum.Checksum;
import dev.langchain4j.agent.tool.ToolSpecification;

import java.util.Objects;

import static com.dbn.common.checksum.ChecksumType.SHA_256;

/**
 * Centralizes defensive bounds for MCP tool metadata before it is cached,
 * rendered in Swing UI, or exposed to assistant execution.
 */
public final class AssistantMcpToolMetadataGuard {
    public static final int MAX_TOOL_COUNT = 256;
    public static final int MAX_TOOL_NAME_LENGTH = 128;
    public static final int MAX_TOOL_DESCRIPTION_LENGTH = 1_024;
    public static final int MAX_TOOL_INSTRUCTION_LENGTH = 4_096;
    public static final int MAX_TOTAL_TOOL_METADATA_LENGTH = 256_000;

    private AssistantMcpToolMetadataGuard() {
    }

    public static AssistantMcpToolInfo createToolInfo(AssistantMcpServer mcpServer, ToolSpecification specification) {
        String toolName = Objects.toString(specification.name(), "");
        String toolDescription = Objects.toString(specification.description(), "");
        String normalizedDescription = limit(toolDescription, MAX_TOOL_INSTRUCTION_LENGTH)
                .replaceAll("(?m)^[ \t]+(?=\\S)", "")
                .trim();

        String rawName = mcpServer.unqualifiedUtilityName(toolName);
        String rawDescription = normalizedDescription.split("\n *\n")[0];
        String name = limit(rawName, MAX_TOOL_NAME_LENGTH);
        String description = limit(rawDescription, MAX_TOOL_DESCRIPTION_LENGTH);
        String instruction = limit(normalizedDescription, MAX_TOOL_INSTRUCTION_LENGTH);
        String signature = createToolSignature(specification);
        boolean truncated =
                !Objects.equals(name, rawName) ||
                !Objects.equals(description, rawDescription) ||
                !Objects.equals(instruction, normalizedDescription);

        return AssistantMcpToolInfo.builder()
                .serverId(mcpServer.getId())
                .name(name)
                .signature(signature)
                .description(description)
                .instruction(instruction)
                .truncated(truncated)
                .build();
    }

    public static boolean isAcceptedToolMetadata(ToolSpecification specification) {
        return
                length(specification.name()) <= MAX_TOOL_NAME_LENGTH &&
                length(specification.description()) <= MAX_TOOL_INSTRUCTION_LENGTH &&
                length(specification.parameters()) <= MAX_TOOL_INSTRUCTION_LENGTH;
    }

    public static int getMetadataLength(ToolSpecification specification) {
        return length(specification.name()) + length(specification.description()) + length(specification.parameters());
    }

    public static int getMetadataLength(AssistantMcpToolInfo toolInfo) {
        return length(toolInfo.getName()) + length(toolInfo.getDescription()) + length(toolInfo.getInstruction());
    }

    private static String createToolSignature(ToolSpecification specification) {
        String contract = String.join("\n",
                Objects.toString(specification.name(), ""),
                Objects.toString(specification.description(), ""),
                Objects.toString(specification.parameters(), ""));
        return Checksum.fromStringContent(contract, SHA_256);
    }

    private static int length(Object value) {
        return Objects.toString(value, "").length();
    }

    private static String limit(String value, int maxLength) {
        if (value == null) return "";
        if (value.length() <= maxLength) return value;
        if (maxLength <= 3) return value.substring(0, maxLength);
        return value.substring(0, maxLength - 3) + "...";
    }
}
