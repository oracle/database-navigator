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
import com.dbn.common.approval.UserApprovalCancelledException;
import com.dbn.common.approval.UserApprovalManager;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static com.dbn.assistant.mcp.model.AssistantMcpServer.qualifiedUtilityName;
import static com.dbn.assistant.mcp.model.AssistantMcpServerType.STDIO;
import static com.dbn.common.approval.UserApprovalAction.MCP_SERVER_ACCESS;
import static com.dbn.nls.NlsResources.txt;

@Slf4j
@UtilityClass
public class AssistantMcpToolProviders {

    private static McpTransport createMcpTransport(AssistantMcpServer mcpServer) {
        AssistantMcpServerVerifier.verify(mcpServer);

        return switch (mcpServer.getType()) {
            case HTTP -> createHttpMcpTransport(mcpServer);
            case STDIO -> createStdioMcpTransport(mcpServer);
        };
    }

    private static McpTransport createHttpMcpTransport(AssistantMcpServer mcpServer) {
        UserApprovalManager.getInstance().ensureApproved(MCP_SERVER_ACCESS, mcpServer);
        if (mcpServer.getType() != AssistantMcpServerType.HTTP) throw new UserApprovalCancelledException();

        StreamableHttpMcpTransport transport = buildHttpMcpTransport(mcpServer);
        return new AssistantMcpGuardedTransport(transport, null);
    }

    private static McpTransport createStdioMcpTransport(AssistantMcpServer mcpServer) {
        AssistantMcpCommand command = approveStdioMcpServer(mcpServer);

        AssistantMcpServer launchServer = mcpServer.clone();
        launchServer.setType(STDIO);
        launchServer.setCommand(command.tokens().get(0));
        launchServer.setCommandArguments(new ArrayList<>(command.tokens().subList(1, command.tokens().size())));

        StdioMcpTransport transport = buildStdioMcpTransport(launchServer);
        return new AssistantMcpGuardedTransport(transport, command);
    }

    private static AssistantMcpCommand approveStdioMcpServer(AssistantMcpServer mcpServer) {
        List<String> commandTokens = List.copyOf(mcpServer.getCommandTokens());
        AssistantMcpCommand beforeApproval = AssistantMcpCommandSignature.capture(commandTokens);

        UserApprovalManager.getInstance().ensureApproved(MCP_SERVER_ACCESS, mcpServer);
        if (mcpServer.getType() != STDIO) throw new UserApprovalCancelledException();
        if (!commandTokens.equals(mcpServer.getCommandTokens())) throw new UserApprovalCancelledException();

        AssistantMcpCommand afterApproval = AssistantMcpCommandSignature.capture(commandTokens);
        if (!beforeApproval.equals(afterApproval)) throw new UserApprovalCancelledException();

        return afterApproval;
    }

    private static StreamableHttpMcpTransport buildHttpMcpTransport(AssistantMcpServer mcpServer) {
        return StreamableHttpMcpTransport
                .builder()
                .url(mcpServer.getUrl())
                .build();
    }

    private static StdioMcpTransport buildStdioMcpTransport(AssistantMcpServer mcpServer) {
        return StdioMcpTransport
                .builder()
                .command(mcpServer.getCommandTokens())
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
        } catch (UserApprovalCancelledException e) {
            throw e;
        } catch (Throwable t) {
            log.warn(t.getMessage(), t);
            errorHandler.accept(txt("msg.assistant.error.McpServerInitializationFailed", serverName), t);
            return null;
        }
    }
}
