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
import com.dbn.common.approval.UserApprovalAdapter;
import com.dbn.common.checksum.Checksum;
import com.dbn.common.util.Messages;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

import static com.dbn.common.checksum.ChecksumType.SHA_256;
import static com.dbn.nls.NlsResources.txt;

public class AssistantMcpServerApprovalAdapter implements UserApprovalAdapter<AssistantMcpServer> {
    private static final String[] APPROVAL_OPTIONS = Messages.options(
            txt("msg.shared.button.TrustAndConnect"),
            txt("msg.shared.button.Cancel"));

    @Override
    public Class<AssistantMcpServer> getApprovalClass() {
        return AssistantMcpServer.class;
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
                mcpServer.getEndpoint());
    }

    @Override
    public String getApprovalKey(AssistantMcpServer mcpServer) {
        return "mcp-server:" + mcpServer.getId().id() + ":" + getEndpointFingerprint(mcpServer);
    }

    @Override
    public String[] getApprovalOptions(AssistantMcpServer approvable) {
        return APPROVAL_OPTIONS;
    }

    private String getEndpointFingerprint(AssistantMcpServer mcpServer) {
        return Checksum.fromStringContent(mcpServer.getType().name() + ":" + mcpServer.getEndpoint(), SHA_256);
    }

    @Override
    @Nullable
    public Duration getRejectionCooldown(AssistantMcpServer approvable) {
        return Duration.ofSeconds(10);
    }
}
