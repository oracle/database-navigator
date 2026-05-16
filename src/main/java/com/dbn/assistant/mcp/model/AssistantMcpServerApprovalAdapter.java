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

import com.dbn.common.approval.UserApprovalAdapter;
import com.dbn.common.checksum.Checksum;

import static com.dbn.common.checksum.ChecksumType.SHA_256;

public class AssistantMcpServerApprovalAdapter implements UserApprovalAdapter<AssistantMcpServer> {
    @Override
    public Class<AssistantMcpServer> getApprovalClass() {
        return AssistantMcpServer.class;
    }

    @Override
    public String getApprovalTitle(AssistantMcpServer mcpServer) {
        return "Approve MCP Server \"" + mcpServer.getName() + "\"";
    }

    @Override
    public String getApprovalMessage(AssistantMcpServer mcpServer) {
        return "DB Assistant wants to use MCP server \"" + mcpServer.getName() + "\".\n\n" +
                "Endpoint type: " + mcpServer.getType().name() + "\n" +
                "Endpoint: " + mcpServer.getEndpoint() + "\n\n" +
                "Only approve this endpoint if you trust this project configuration.";
    }

    @Override
    public String getApprovalKey(AssistantMcpServer mcpServer) {
        return "mcp-server:" + mcpServer.getId().id() + ":" + getEndpointFingerprint(mcpServer);
    }

    private String getEndpointFingerprint(AssistantMcpServer mcpServer) {
        return Checksum.fromStringContent(mcpServer.getType().name() + ":" + mcpServer.getEndpoint(), SHA_256);
    }
}
