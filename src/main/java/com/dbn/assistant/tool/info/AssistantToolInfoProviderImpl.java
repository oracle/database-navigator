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

package com.dbn.assistant.tool.info;

import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.state.AssistantStateExtension;
import com.dbn.assistant.tool.AssistantTool;
import com.dbn.assistant.tool.AssistantToolCache;
import com.dbn.assistant.tool.AssistantToolCategory;
import com.dbn.assistant.tool.AssistantToolInfo.UtilitySpec;
import com.dbn.assistant.tool.AssistantToolType;
import com.dbn.assistant.tool.approval.AssistantToolApprovals;
import com.dbn.assistant.tool.execution.AssistantToolInvocation;
import com.dbn.assistant.tool.execution.AssistantToolRequest;
import com.dbn.assistant.tool.execution.AssistantToolResponse;
import com.dbn.common.util.Lists;
import com.dbn.common.util.Strings;
import com.dbn.common.util.Unsafe;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

import static com.dbn.assistant.tool.AssistantToolData.getUtilitySpec;

public class AssistantToolInfoProviderImpl extends AssistantStateExtension implements AssistantToolInfoProvider {
    private final AssistantToolInvocation invocation;

    public AssistantToolInfoProviderImpl(@NotNull AssistantState assistantState, AssistantToolInvocation invocation) {
        super(assistantState);
        this.invocation = invocation;
    }

    @Override
    public String getToolName() {
        String utilityName = getToolRequest().getToolName();

        AssistantTool tool = getTool();
        UtilitySpec utilitySpec = getUtilitySpec(tool, utilityName);
        if (utilitySpec == null) return utilityName;

        return utilitySpec.name();
    }

    @Override
    public String getToolDescription() {
        String utilityName = getToolRequest().getToolName();

        AssistantTool tool = getTool();
        UtilitySpec utilitySpec = getUtilitySpec(tool, utilityName);
        if (utilitySpec == null) return "";
        return utilitySpec.description();
    }

    @Override
    public String getToolTypeName() {
        return getTool().getName();
    }

    @Override
    public String getToolTypeDescription() {
        return getTool().getDescription();
    }

    @Override
    public String getToolCategoryName() {
        return getToolCategory().getName();
    }

    @Override
    public String getToolCategoryDescription() {
        return getToolCategory().getDescription();
    }

    @Override
    public String getToolRequestSummary() {
        return Unsafe.logged("", () -> buildToolRequestSummary());
    }

    private @NotNull String buildToolRequestSummary() {
        AssistantToolRequest request = getToolRequest();
        String utility = request.getToolName();

        AssistantTool tool = getTool();
        UtilitySpec utilitySpec = getUtilitySpec(tool, utility);
        if (utilitySpec == null) return "";

        String summary = utilitySpec.summary();
        if (summary == null) return "";

        int placeholderCount =  Strings.countOccurrences(summary, "%s");
        if (placeholderCount == 0) return "";

        List<?> values = request.getToolArgumentValues();
        if (values.size() < placeholderCount) return "";

        Object[] arguments = values.subList(0, placeholderCount).toArray(new Object[0]);
        for (int i = 0; i < arguments.length; i++) {
            Object argument = arguments[i];
            if (argument instanceof List<?> list) {
                arguments[i] = Lists.toCsv(list, e -> Objects.toString(e));
            }
        }

        return "(" + String.format(summary, arguments) + ")";
    }

    @Override
    public String getToolResponseSummary() {
        return "";
    }

    private AssistantTool getTool() {
        AssistantToolCache toolCache = getToolCache();

        String utilityName = getToolRequest().getToolName();
        return toolCache.getAssistantTool(utilityName);
    }

    public AssistantToolCategory getToolCategory() {
        return getTool().getCategory();
    }

    @Override
    public AssistantToolType getToolType() {
        return getTool().getType();
    }

    private AssistantToolRequest getToolRequest() {
        return invocation.getRequest();
    }

    private AssistantToolResponse getToolResponse() {
        return invocation.getResponse();
    }

    private AssistantToolCache getToolCache() {
        AssistantState assistantState = getAssistantState();
        return AssistantToolCache.get(assistantState);
    }

    private AssistantToolApprovals getToolApprovals() {
        AssistantState state = getAssistantState();
        return state.getToolApprovals();
    }
}
