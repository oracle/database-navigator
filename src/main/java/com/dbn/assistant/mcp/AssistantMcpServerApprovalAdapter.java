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
import com.dbn.common.approval.UserApprovalAction;
import com.dbn.common.approval.UserApprovalAdapter;
import com.dbn.common.approval.UserApprovalOption;
import com.dbn.common.checksum.Checksum;
import com.dbn.common.util.Sockets;

import java.net.URI;
import java.net.UnknownHostException;

import static com.dbn.common.approval.UserApprovalAction.MCP_SERVER_ACCESS;
import static com.dbn.common.checksum.ChecksumType.SHA_256;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.nls.NlsResources.txt;

/**
 * Prepares user approval information for allowing the assistant to connect to configured
 * MCP servers and exchange tool requests with them.
 */
public class AssistantMcpServerApprovalAdapter implements UserApprovalAdapter<AssistantMcpServer> {
    private static final UserApprovalOption[] APPROVAL_OPTIONS = {
            UserApprovalOption.one(txt("msg.shared.button.TrustAndConnect")),
            UserApprovalOption.none(txt("msg.shared.button.Cancel"))};

    @Override
    public Class<AssistantMcpServer> getApprovalClass() {
        return AssistantMcpServer.class;
    }

    @Override
    public UserApprovalAction getApprovalAction() {
        return MCP_SERVER_ACCESS;
    }

    @Override
    public String getApprovalTitle(AssistantMcpServer mcpServer) {
        return txt("msg.assistant.title.TrustMcpServer");
    }

    @Override
    public String getApprovalMessage(AssistantMcpServer mcpServer) {
        return txt("msg.assistant.question.TrustMcpServer",
                mcpServer.getName(),
                mcpServer.getType().name(),
                mcpServer.getEndpoint(),
                getEndpointWarning(mcpServer));
    }

    @Override
    public String getApprovalKey(AssistantMcpServer mcpServer) {
        return "mcp-server:" + mcpServer.getId().id() + ":" + getEndpointFingerprint(mcpServer);
    }

    @Override
    public UserApprovalOption[] getApprovalOptions(AssistantMcpServer approvable) {
        return APPROVAL_OPTIONS;
    }

    private String getEndpointFingerprint(AssistantMcpServer mcpServer) {
        String content = buildFingerprintContent(mcpServer);
        return Checksum.fromStringContent(content, SHA_256);
    }

    private String buildFingerprintContent(AssistantMcpServer mcpServer) {
        StringBuilder builder = new StringBuilder();
        appendFingerprintToken(builder, mcpServer.getType().name());

        switch (mcpServer.getType()) {
            case HTTP -> appendFingerprintToken(builder, mcpServer.getUrl());
            case STDIO -> mcpServer.getCommandTokens().forEach(t -> appendFingerprintToken(builder, t));
        }
        return builder.toString();
    }

    private static void appendFingerprintToken(StringBuilder builder, String token) {
        if (token == null) token = "";
        builder.append(token.length()).append(':').append(token);
    }

    private static String getEndpointWarning(AssistantMcpServer mcpServer) {
        if (mcpServer.getType() != AssistantMcpServerType.HTTP) return "";
        if (isEmpty(mcpServer.getUrl())) return "";

        try {
            URI uri = URI.create(mcpServer.getUrl());
            String host = uri.getHost();
            if (isEmpty(host)) return "";

            return Sockets.isLocalNetworkHost(host) ?
                    txt("msg.assistant.warning.McpServerLocalNetworkEndpoint") : "";
        } catch (IllegalArgumentException | UnknownHostException e) {
            return "";
        }
    }
}
