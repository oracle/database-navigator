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
import com.dbn.assistant.mcp.model.AssistantMcpServerType;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static com.dbn.assistant.mcp.model.AssistantMcpServer.qualifiedUtilityName;

@Slf4j
@UtilityClass
public class AssistantMcpToolProviders {

    private static McpTransport createMcpTransport(AssistantMcpServer mcpServer) {
        AssistantMcpServerType type = mcpServer.getType();
        return switch (type) {
            case HTTP -> createHttpMcpTransport(mcpServer);
            case STDIO -> createStdioMcpTransport(mcpServer);
        };
    }

    private static StdioMcpTransport createStdioMcpTransport(AssistantMcpServer mcpServer) {
        return StdioMcpTransport
                .builder()
                .command(mcpServer.getCommandTokens())
                .build();
    }

    private static McpTransport createHttpMcpTransport(AssistantMcpServer mcpServer) {
        return StreamableHttpMcpTransport
                .builder()
                .url(mcpServer.getUrl())
                .build();
    }

    private static McpClient createMcpClient(AssistantMcpServer mcpServer) {
        McpTransport transport = createMcpTransport(mcpServer);
        return DefaultMcpClient.builder()
                .key(mcpServer.getKey())
                .transport(transport)
                .build();
    }

    public static ToolProvider createToolProvider(
            AssistantMcpServer mcpServer,
            BiConsumer<String, Throwable> errorHandler,
            BiPredicate<McpClient, ToolSpecification> toolsFilter,
            Function<ToolExecutor, ToolExecutor> toolWrapper) {
        String serverName = mcpServer.getName();
        try {
            McpClient mcpClient = createMcpClient(mcpServer);

            return McpToolProvider.builder()
                    .mcpClients(mcpClient)
                    .toolNameMapper((c, s) -> qualifiedUtilityName(c.key(), s.name()))
                    .toolWrapper(toolWrapper)
                    .filter(toolsFilter)
                    .build();
        } catch (Throwable t) {
            log.warn(t.getMessage(), t);
            errorHandler.accept("Failed to initialize MCP Server \"" + serverName + "\"", t);
            return null;
        }
    }
}
