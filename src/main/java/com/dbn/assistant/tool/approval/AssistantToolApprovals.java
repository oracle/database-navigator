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

package com.dbn.assistant.tool.approval;

import com.dbn.assistant.tool.AssistantTool;
import com.dbn.assistant.tool.AssistantToolCategory;
import com.dbn.assistant.tool.AssistantToolType;
import com.dbn.common.sign.Signed;
import com.dbn.common.state.PersistentStateElement;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.dbn.assistant.tool.AssistantToolData.getToolCategory;
import static com.dbn.assistant.tool.AssistantToolData.getToolTypes;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.APPROVED;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.BLOCKED;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.PROMPTED;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.util.Commons.nvl;

public class AssistantToolApprovals implements PersistentStateElement, Signed {
    private final Map<AssistantToolType, AssistantToolApprovalStatus> types = new ConcurrentHashMap<>();
    private final Map<AssistantToolCategory, AssistantToolApprovalStatus> categories = new ConcurrentHashMap<>();

    private final AtomicInteger signature = new AtomicInteger(0);

    public boolean isApproved(AssistantTool tool) {
        if (isBlocked(tool.getCategory())) return false;
        if (isApproved(tool.getType())) return true;
        if (tool.isExternal()) return true;

        return false;
    }

    public int countBlockedTools(List<AssistantToolType> types) {
        int count = 0;
        for (AssistantToolType type : types) {
            if (isBlocked(type)) count++;
        }
        return count;
    }

    private void updateSignature() {
        signature.incrementAndGet();
    }

    public int getSignature() {
        return signature.get();
    }

    public AssistantToolApprovalStatus getStatus(AssistantToolCategory category) {
        return nvl(categories.get(category), PROMPTED);
    }

    public AssistantToolApprovalStatus getStatus(AssistantToolType type) {
        return nvl(types.get(type), PROMPTED);
    }

    public boolean isApproved(@NotNull AssistantToolType type) {
        return types.get(type) == APPROVED;
    }

    public boolean isApproved(@NotNull AssistantToolCategory category) {
        return categories.get(category) == APPROVED;
    }

    public boolean isBlocked(@NotNull AssistantToolType type) {
        return types.get(type) == BLOCKED;
    }

    public boolean isBlocked(@NotNull AssistantToolCategory category) {
        return categories.get(category) == BLOCKED;
    }

    public boolean isEmpty() {
        return categories.isEmpty() && types.isEmpty();
    }

    public void setStatus(AssistantToolCategory category, AssistantToolApprovalStatus status) {
        categories.put(category, status);
        updateSignature();
    }

    public void setStatus(AssistantToolType type, AssistantToolApprovalStatus status) {
        types.put(type, status);

        updateCategoryStatus(type);
        updateSignature();
    }

    private void updateCategoryStatus(AssistantToolType type) {
        Set<AssistantToolApprovalStatus> approvalStatuses = new HashSet<>();

        AssistantToolCategory category = getToolCategory(type);
        List<AssistantToolType> toolTypes = getToolTypes(category);
        for (AssistantToolType toolType : toolTypes) {
            AssistantToolApprovalStatus approvalStatus = getStatus(toolType);
            approvalStatuses.add(approvalStatus);
        }

        // if all types share the same status, update the category status as well
        if (approvalStatuses.size() == 1) {
            AssistantToolApprovalStatus sharedStatus = approvalStatuses.iterator().next();
            if (sharedStatus.isOneOf(PROMPTED, APPROVED)) {
                setStatus(category, sharedStatus);
            }
        }
    }

    @Override
    public void readState(Element element) {
        if (element == null) return;

        Element categoriesElement = element.getChild("categories");
        List<Element> categoryElements = childrenOf(categoriesElement);
        for (Element categoryElement : categoryElements) {
            AssistantToolCategory toolCategory = enumAttribute(categoryElement, "id", AssistantToolCategory.class);
            if (toolCategory == null) continue; // ignore renamed tool categories

            AssistantToolApprovalStatus approvalStatus = enumAttribute(categoryElement, "status", AssistantToolApprovalStatus.class);
            categories.put(toolCategory, approvalStatus);
        }

        Element typesElement = element.getChild("types");
        List<Element> typeElements = childrenOf(typesElement);
        for (Element typeElement : typeElements) {
            AssistantToolType toolType = enumAttribute(typeElement, "id", AssistantToolType.class);
            if (toolType == null) continue; // ignore renamed tool types

            AssistantToolApprovalStatus approvalStatus = enumAttribute(typeElement, "status", AssistantToolApprovalStatus.class);
            types.put(toolType, approvalStatus);
        }
    }

    @Override
    public void writeState(Element element) {
        if (element == null) return;

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
        }
    }
}
