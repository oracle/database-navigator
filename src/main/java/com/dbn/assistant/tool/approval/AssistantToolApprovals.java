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

import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.state.AssistantStateExtension;
import com.dbn.assistant.tool.AssistantToolCategory;
import com.dbn.assistant.tool.AssistantToolType;
import com.dbn.common.action.UserDataKeys;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.dbn.common.action.UserDataKeys.ASSISTANT_TOOL_APPROVALS;

public class AssistantToolApprovals extends AssistantStateExtension {
    private final Map<AssistantToolType, Boolean> types = new ConcurrentHashMap<>();
    private final Map<AssistantToolCategory, Boolean> categories = new ConcurrentHashMap<>();

    private final AtomicInteger signature = new AtomicInteger(0);

    private AssistantToolApprovals(@NotNull AssistantState assistantState) {
        super(assistantState);
    }

    public static AssistantToolApprovals get(AssistantState assistantState) {
        return UserDataKeys.getUserDataSync(assistantState, ASSISTANT_TOOL_APPROVALS, () -> new AssistantToolApprovals(assistantState));
    }


    private int updateSignature() {
        return signature.incrementAndGet();
    }

    public int getSignature() {
        return signature.get();
    }

    public void allow(@NotNull AssistantToolType type) {
        types.put(type, true);
        updateSignature();
    }

    public void deny(@NotNull AssistantToolType type) {
        types.put(type, false);
        updateSignature();
    }

    public void allow(@NotNull AssistantToolCategory category) {
        categories.put(category, true);
        updateSignature();
    }

    public void deny(@NotNull AssistantToolCategory category) {
        categories.put(category, false);
        updateSignature();
    }

    public boolean isAllowed(@NotNull AssistantToolType type) {
        Boolean state = types.get(type);
        return state != null && state;
    }

    public boolean isAllowed(@NotNull AssistantToolCategory category) {
        Boolean state = categories.get(category);
        return state != null && state;
    }

    public boolean isDenied(@NotNull AssistantToolType type) {
        Boolean state = types.get(type);
        return state != null && !state;
    }

    public boolean isDenied(@NotNull AssistantToolCategory category) {
        Boolean state = categories.get(category);
        return state != null && !state;
    }
}
