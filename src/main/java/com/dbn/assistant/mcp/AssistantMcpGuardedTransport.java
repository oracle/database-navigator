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
import dev.langchain4j.mcp.client.McpCallContext;
import dev.langchain4j.mcp.client.transport.McpOperationHandler;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.protocol.McpClientMessage;
import dev.langchain4j.mcp.protocol.McpClientMethod;
import dev.langchain4j.mcp.protocol.McpInitializeRequest;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static com.dbn.assistant.mcp.model.AssistantMcpToolMetadataGuard.MAX_TOOL_COUNT;
import static com.dbn.assistant.mcp.model.AssistantMcpToolMetadataGuard.MAX_TOOL_INSTRUCTION_LENGTH;
import static com.dbn.assistant.mcp.model.AssistantMcpToolMetadataGuard.MAX_TOOL_NAME_LENGTH;
import static com.dbn.assistant.mcp.model.AssistantMcpToolMetadataGuard.MAX_TOTAL_TOOL_METADATA_LENGTH;

/**
 * Bounds MCP tools/list metadata before LangChain4j materializes it into tool specifications.
 * <p>
 * The wrapped LangChain4j transports still parse the raw server response into a {@link JsonNode}
 * before this guard runs. This guard prevents the next amplification step - unbounded
 * ToolSpecification creation, caching, DBN storage, and Swing rendering - until LangChain4j
 * exposes raw response or streaming limits.
 */
class AssistantMcpGuardedTransport implements McpTransport {
    private final McpTransport delegate;

    AssistantMcpGuardedTransport(McpTransport delegate) {
        this.delegate = delegate;
    }

    @Override
    public void start(McpOperationHandler messageHandler) {
        delegate.start(messageHandler);
    }

    @Override
    public CompletableFuture<JsonNode> initialize(McpInitializeRequest request) {
        return delegate.initialize(request);
    }

    @Override
    public CompletableFuture<JsonNode> executeOperationWithResponse(McpClientMessage request) {
        return guard(request, delegate.executeOperationWithResponse(request));
    }

    @Override
    public CompletableFuture<JsonNode> executeOperationWithResponse(McpCallContext context) {
        return guard(context.message(), delegate.executeOperationWithResponse(context));
    }

    @Override
    public void executeOperationWithoutResponse(McpClientMessage request) {
        delegate.executeOperationWithoutResponse(request);
    }

    @Override
    public void executeOperationWithoutResponse(McpCallContext context) {
        delegate.executeOperationWithoutResponse(context);
    }

    @Override
    public void checkHealth() {
        delegate.checkHealth();
    }

    @Override
    public void onFailure(Runnable actionOnFailure) {
        delegate.onFailure(actionOnFailure);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    private static CompletableFuture<JsonNode> guard(McpClientMessage request, CompletableFuture<JsonNode> response) {
        if (request.method != McpClientMethod.TOOLS_LIST) return response;

        return response.thenApply(AssistantMcpGuardedTransport::guardToolMetadata);
    }

    static JsonNode guardToolMetadata(JsonNode response) {
        if (response == null) return null;
        if (!response.isObject()) return response;

        JsonNode result = response.get("result");
        if (result == null) return response;
        if (!result.isObject()) return response;

        JsonNode tools = result.get("tools");
        if (tools == null) return response;
        if (!tools.isArray()) return response;

        ObjectNode guardedResponse = response.deepCopy();
        ObjectNode guardedResult = (ObjectNode) guardedResponse.get("result");
        ArrayNode acceptedTools = JsonNodeFactory.instance.arrayNode();
        int metadataSize = 0;

        for (JsonNode tool : tools) {
            if (acceptedTools.size() >= MAX_TOOL_COUNT) break;
            if (!isAcceptedTool(tool)) continue;

            int toolMetadataSize = getMetadataSize(tool);
            if (metadataSize + toolMetadataSize > MAX_TOTAL_TOOL_METADATA_LENGTH) continue;

            metadataSize += toolMetadataSize;
            acceptedTools.add(tool.deepCopy());
        }

        guardedResult.set("tools", acceptedTools);
        return guardedResponse;
    }

    private static boolean isAcceptedTool(JsonNode tool) {
        JsonNode name = tool.get("name");
        JsonNode inputSchema = tool.get("inputSchema");
        return
                tool.isObject() &&
                name != null &&
                name.isTextual() &&
                textLength(name) > 0 &&
                textLength(name) <= MAX_TOOL_NAME_LENGTH &&
                inputSchema != null &&
                inputSchema.isObject() &&
                textLength(tool.get("description")) <= MAX_TOOL_INSTRUCTION_LENGTH &&
                nodeLength(inputSchema) <= MAX_TOOL_INSTRUCTION_LENGTH;
    }

    private static int getMetadataSize(JsonNode tool) {
        return textLength(tool.get("name")) + textLength(tool.get("description")) + nodeLength(tool.get("inputSchema"));
    }

    private static int textLength(JsonNode node) {
        if (node == null || node.isNull()) return 0;
        return node.asText("").length();
    }

    private static int nodeLength(JsonNode node) {
        if (node == null || node.isNull()) return 0;
        return node.toString().length();
    }
}
