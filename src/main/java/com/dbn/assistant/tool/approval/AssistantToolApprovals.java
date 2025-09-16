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
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.APPROVED;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.DISABLED;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.PROMPTED;
import static com.dbn.common.util.Commons.nvl;

public class AssistantToolApprovals {
    private final Map<AssistantToolType, AssistantToolApprovalStatus> types = new ConcurrentHashMap<>();
    private final Map<AssistantToolCategory, AssistantToolApprovalStatus> categories = new ConcurrentHashMap<>();

    private final AtomicInteger signature = new AtomicInteger(0);

    public boolean isApproved(AssistantTool tool) {
        if (isApproved(tool.getCategory())) return true;
        if (isApproved(tool.getType())) return true;

        return false;
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

    public boolean isDisabled(@NotNull AssistantToolType type) {
        return types.get(type) == DISABLED;
    }

    public boolean isDisabled(@NotNull AssistantToolCategory category) {
        return categories.get(category) == DISABLED;
    }

    public void setStatus(AssistantToolCategory category, AssistantToolApprovalStatus status) {
        categories.put(category, status);
        updateSignature();
    }

    public void setStatus(AssistantToolType type, AssistantToolApprovalStatus status) {
        types.put(type, status);
        updateSignature();
    }
}
