/*
 * Copyright 2025 Oracle and/or its affiliates
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
import com.dbn.common.sign.Signed;
import com.dbn.common.state.PersistentStateElement;
import org.jdom.Element;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.APPROVED;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.BLOCKED;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

public class AssistantMcpToolApprovals implements PersistentStateElement, Signed {
    private final Map<String, AssistantToolApprovalStatus> servers = new ConcurrentHashMap<>();
    private final Map<String, Map<String, AssistantToolApprovalStatus>> tools = new ConcurrentHashMap<>();

    private final AtomicInteger signature = new AtomicInteger(0);


    private void updateSignature() {
        signature.incrementAndGet();
    }

    public int getSignature() {
        return signature.get();
    }

    public void setStatus(String serverKey, String toolName, AssistantToolApprovalStatus status) {
        Map<String, AssistantToolApprovalStatus> approvals = tools.computeIfAbsent(serverKey, k -> new ConcurrentHashMap<>());
        approvals.put(toolName, status);
        updateSignature();
    }

    public void setStatus(String serverKey, AssistantToolApprovalStatus status) {
        servers.put(serverKey, status);
        updateSignature();
    }

    public boolean isApproved(String serverKey, String toolName) {
        Map<String, AssistantToolApprovalStatus> approvals = tools.get(serverKey);
        AssistantToolApprovalStatus toolStatus = approvals == null ? null : approvals.get(toolName);
        if (toolStatus == APPROVED) return true;

        AssistantToolApprovalStatus serverStatus = servers.get(serverKey);
        if (serverStatus == APPROVED) return true;

        return false;
    }

    public boolean isBlocked(String serverKey, String toolName) {
        Map<String, AssistantToolApprovalStatus> approvals = tools.get(serverKey);
        AssistantToolApprovalStatus toolStatus = approvals == null ? null : approvals.get(toolName);
        if (toolStatus == BLOCKED) return true;

        return isBlocked(serverKey);
    }

    public boolean isBlocked(String serverKey) {
        AssistantToolApprovalStatus serverStatus = servers.get(serverKey);
        return serverStatus == BLOCKED;
    }

    public boolean isEmpty() {
        return tools.isEmpty() && servers.isEmpty();
    }

    @Override
    public void readState(Element element) {
        if (element == null) return;

        Element serversElement = element.getChild("servers");
        List<Element> serverElements = childrenOf(serversElement);
        for (Element serverElement : serverElements) {
            String serverKey = stringAttribute(serverElement , "key");
            AssistantToolApprovalStatus approvalStatus = enumAttribute(serverElement, "status", AssistantToolApprovalStatus.class);
            servers.put(serverKey, approvalStatus);
        }

        Element toolsElement = element.getChild("tools");
        List<Element> toolElements = childrenOf(toolsElement);
        for (Element toolElement : toolElements) {
            String serverKey = stringAttribute(toolElement , "server-key");
            String toolName = stringAttribute(toolElement , "tool-name");
            AssistantToolApprovalStatus approvalStatus = enumAttribute(toolElement, "status", AssistantToolApprovalStatus.class);
            Map<String, AssistantToolApprovalStatus> toolApprovals = tools.computeIfAbsent(serverKey, k -> new ConcurrentHashMap<>());
            toolApprovals.put(toolName, approvalStatus);
        }
    }

    @Override
    public void writeState(Element element) {
        if (element == null) return;

        if (!servers.isEmpty()) {
            Element serversElement = newElement(element, "servers");
            for (String serverKey : servers.keySet()) {
                AssistantToolApprovalStatus approvalStatus = servers.get(serverKey);

                Element serverElement = newElement(serversElement, "server");
                setStringAttribute(serverElement, "key", serverKey);
                setEnumAttribute(serverElement, "status", approvalStatus);
            }
        }

        if (!tools.isEmpty()) {
            Element toolsElement = newElement(element, "tools");
            for (String serverKey : tools.keySet()) {
                Map<String, AssistantToolApprovalStatus> toolApprovals = tools.get(serverKey);
                for (String toolName : toolApprovals.keySet()) {
                    AssistantToolApprovalStatus approvalStatus = toolApprovals.get(toolName);

                    Element toolElement = newElement(toolsElement, "tool");
                    setStringAttribute(toolElement, "server-key", serverKey);
                    setStringAttribute(toolElement, "tool-name", toolName);
                    setEnumAttribute(toolElement, "status", approvalStatus);
                }
            }
        }
    }
}
