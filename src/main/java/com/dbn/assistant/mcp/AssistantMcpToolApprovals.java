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

import com.dbn.assistant.tool.approval.AssistantToolApprovalStatus;
import com.dbn.common.EntityId;
import com.dbn.common.sign.Signed;
import com.dbn.common.state.PersistentStateElement;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.APPROVED;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.BLOCKED;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.PROMPTED;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.constantAttribute;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setConstantAttribute;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

public class AssistantMcpToolApprovals implements PersistentStateElement, Signed {
    private final Map<EntityId, AssistantToolApprovalStatus> servers = new ConcurrentHashMap<>();
    private final Map<EntityId, Map<String, AssistantToolApprovalStatus>> tools = new ConcurrentHashMap<>();

    private final AtomicInteger signature = new AtomicInteger(0);


    private void updateSignature() {
        signature.incrementAndGet();
    }

    public int getSignature() {
        return signature.get();
    }

    public void setStatus(EntityId serverId, String toolName, AssistantToolApprovalStatus status) {
        Map<String, AssistantToolApprovalStatus> approvals = tools.computeIfAbsent(serverId, k -> new ConcurrentHashMap<>());
        approvals.put(toolName, status);
        updateSignature();
    }

    public void setStatus(EntityId serverId, AssistantToolApprovalStatus status) {
        servers.put(serverId, status);
        tools.remove(serverId);
        updateSignature();
    }

    public void setStatus(EntityId serverId, List<String> toolNames, AssistantToolApprovalStatus status) {
        if (status == APPROVED) {
            servers.put(serverId, status);
        } else {
            servers.remove(serverId);
        }

        Map<String, AssistantToolApprovalStatus> approvals = tools.computeIfAbsent(serverId, k -> new ConcurrentHashMap<>());
        for (String toolName : toolNames) {
            approvals.put(toolName, status);
        }
        updateSignature();
    }

    public boolean isApproved(EntityId serverId, String toolName) {
        Map<String, AssistantToolApprovalStatus> approvals = tools.get(serverId);
        AssistantToolApprovalStatus toolStatus = approvals == null ? null : approvals.get(toolName);
        if (toolStatus == APPROVED) return true;

        return false;
    }

    public boolean isBlocked(EntityId serverId, String toolName) {
        AssistantToolApprovalStatus toolStatus = getStatus(serverId, toolName);
        if (toolStatus == BLOCKED) return true;

        return isBlocked(serverId);
    }

    @NotNull
    public AssistantToolApprovalStatus getStatus(EntityId serverId, String toolName) {
        Map<String, AssistantToolApprovalStatus> approvals = tools.get(serverId);
        AssistantToolApprovalStatus approvalStatus = approvals == null ? null : approvals.get(toolName);
        if (approvalStatus == null) {
            approvalStatus = servers.get(serverId);
            if (approvalStatus == APPROVED) {
                approvalStatus = PROMPTED;
            }
        }

        return approvalStatus == null ? PROMPTED : approvalStatus;
    }

    @NotNull
    public AssistantToolApprovalStatus getStatus(EntityId serverId) {
        AssistantToolApprovalStatus approvalStatus = servers.get(serverId);
        return approvalStatus == null ? PROMPTED : approvalStatus;
    }

    public boolean isBlocked(EntityId serverId) {
        AssistantToolApprovalStatus serverStatus = servers.get(serverId);
        return serverStatus == BLOCKED;
    }

    public boolean isEmpty() {
        return tools.isEmpty() && servers.isEmpty();
    }

    @Override
    public void readState(Element element) {
        servers.clear();
        tools.clear();
        if (element == null) return;

        Element serversElement = element.getChild("servers");
        List<Element> serverElements = childrenOf(serversElement);
        for (Element serverElement : serverElements) {
            EntityId serverId = constantAttribute(serverElement , "id", EntityId.class);
            AssistantToolApprovalStatus approvalStatus = enumAttribute(serverElement, "status", AssistantToolApprovalStatus.class);
            servers.put(serverId, approvalStatus);
        }

        Element toolsElement = element.getChild("tools");
        List<Element> toolElements = childrenOf(toolsElement);
        for (Element toolElement : toolElements) {
            EntityId serverId = constantAttribute(toolElement , "server-id", EntityId.class);
            String toolName = stringAttribute(toolElement , "tool-name");
            AssistantToolApprovalStatus approvalStatus = enumAttribute(toolElement, "status", AssistantToolApprovalStatus.class);
            Map<String, AssistantToolApprovalStatus> toolApprovals = tools.computeIfAbsent(serverId, k -> new ConcurrentHashMap<>());
            toolApprovals.put(toolName, approvalStatus);
        }
    }

    @Override
    public void writeState(Element element) {
        if (element == null) return;

        if (!servers.isEmpty()) {
            Element serversElement = newElement(element, "servers");
            for (EntityId serverId : servers.keySet()) {
                AssistantToolApprovalStatus approvalStatus = servers.get(serverId);

                Element serverElement = newElement(serversElement, "server");
                setConstantAttribute(serverElement, "id", serverId);
                setEnumAttribute(serverElement, "status", approvalStatus);
            }
        }

        if (!tools.isEmpty()) {
            Element toolsElement = newElement(element, "tools");
            for (EntityId entityId : tools.keySet()) {
                Map<String, AssistantToolApprovalStatus> toolApprovals = tools.get(entityId);
                for (String toolName : toolApprovals.keySet()) {
                    AssistantToolApprovalStatus approvalStatus = toolApprovals.get(toolName);

                    Element toolElement = newElement(toolsElement, "tool");
                    setConstantAttribute(toolElement, "server-id", entityId);
                    setStringAttribute(toolElement, "tool-name", toolName);
                    setEnumAttribute(toolElement, "status", approvalStatus);
                }
            }
        }
    }
}
