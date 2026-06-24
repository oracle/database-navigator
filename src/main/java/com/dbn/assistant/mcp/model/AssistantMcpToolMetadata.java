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

import dev.langchain4j.agent.tool.ToolSpecification;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import static com.dbn.assistant.mcp.model.AssistantMcpToolMetadataGuard.MAX_TOOL_COUNT;
import static com.dbn.assistant.mcp.model.AssistantMcpToolMetadataGuard.MAX_TOTAL_TOOL_METADATA_LENGTH;
import static com.dbn.assistant.mcp.model.AssistantMcpToolMetadataGuard.getMetadataLength;
import static com.dbn.assistant.mcp.model.AssistantMcpToolMetadataGuard.isAcceptedToolMetadata;

@Getter
public class AssistantMcpToolMetadata {
    private final List<AssistantMcpToolInfo> tools = new ArrayList<>();
    private int available;
    private int skipped;
    private int metadataSize;

    public boolean accept(ToolSpecification specification) {
        available++;

        if (!isAcceptedToolMetadata(specification)) return skip();

        int length = getMetadataLength(specification);
        if (tools.size() >= MAX_TOOL_COUNT) return skip();
        if (metadataSize + length > MAX_TOTAL_TOOL_METADATA_LENGTH) return skip();

        metadataSize += length;
        return true;
    }

    public boolean addTool(ToolSpecification specification, Supplier<AssistantMcpToolInfo> toolInfo) {
        if (!accept(specification)) return false;

        tools.add(toolInfo.get());
        return true;
    }

    public void sortTools() {
        tools.sort(Comparator.comparing(AssistantMcpToolInfo::getName));
    }

    public List<AssistantMcpToolInfo> getTools() {
        return List.copyOf(tools);
    }

    public boolean isTruncated() {
        return skipped > 0;
    }

    private boolean skip() {
        skipped++;
        return false;
    }

    public int getToolCount() {
        return tools.size();
    }
}
