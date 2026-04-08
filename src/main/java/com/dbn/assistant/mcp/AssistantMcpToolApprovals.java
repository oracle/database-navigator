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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.APPROVED;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.BLOCKED;

public class AssistantMcpToolApprovals implements PersistentStateElement, Signed {
    private final Map<String, Map<String, AssistantToolApprovalStatus>> tools = new ConcurrentHashMap<>();
    private final Map<String, AssistantToolApprovalStatus> servers = new ConcurrentHashMap<>();

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
        if (approvals == null) return false;

        AssistantToolApprovalStatus toolStatus = approvals.get(toolName);
        if (toolStatus == APPROVED) return true;

        AssistantToolApprovalStatus serverStatus = servers.get(serverKey);
        if (serverStatus == APPROVED) return true;

        return false;
    }

    public boolean isBlocked(String serverKey, String toolName) {
        Map<String, AssistantToolApprovalStatus> approvals = tools.get(serverKey);
        if (approvals == null) return false;

        AssistantToolApprovalStatus toolStatus = approvals.get(toolName);
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
/*        if (element == null) return;

        Element categoriesElement = element.getChild("categories");
        List<Element> categoryElements = childrenOf(categoriesElement);
        for (Element categoryElement : categoryElements) {
            AssistantToolCategory toolCategory = enumAttribute(categoryElement, "id", AssistantToolCategory.class);
            AssistantToolApprovalStatus approvalStatus = enumAttribute(categoryElement, "status", AssistantToolApprovalStatus.class);
            categories.put(toolCategory, approvalStatus);
        }

        Element typesElement = element.getChild("types");
        List<Element> typeElements = childrenOf(typesElement);
        for (Element typeElement : typeElements) {
            AssistantToolType toolType = enumAttribute(typeElement, "id", AssistantToolType.class);
            AssistantToolApprovalStatus approvalStatus = enumAttribute(typeElement, "status", AssistantToolApprovalStatus.class);
            types.put(toolType, approvalStatus);
        }*/
    }

    @Override
    public void writeState(Element element) {
/*        if (element == null) return;

        if (!categories.isEmpty()) {
            Element categoriesElement = newElement(element, "categories");
            for (AssistantToolCategory toolCategory : categories.keySet()) {
                AssistantToolApprovalStatus approvalStatus = categories.get(toolCategory);

                Element categoryElement = newElement(categoriesElement, "category");
                setEnumAttribute(categoryElement, "id", toolCategory);
                setEnumAttribute(categoryElement, "status", approvalStatus);
            }
        }

        if (!types.isEmpty()) {
            Element typesElement = newElement(element, "types");
            for (AssistantToolType toolType : types.keySet()) {
                AssistantToolApprovalStatus approvalStatus = types.get(toolType);

                Element typeElement = newElement(typesElement, "type");
                setEnumAttribute(typeElement, "id", toolType);
                setEnumAttribute(typeElement, "status", approvalStatus);
            }
        }*/
    }
}
