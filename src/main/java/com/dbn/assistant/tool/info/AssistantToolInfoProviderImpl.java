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
import com.dbn.assistant.tool.AssistantToolInfo;
import com.dbn.assistant.tool.AssistantToolType;
import com.dbn.assistant.tool.approval.AssistantToolApprovals;
import com.dbn.assistant.tool.execution.AssistantToolInvocation;
import com.dbn.assistant.tool.execution.AssistantToolRequest;
import com.dbn.assistant.tool.execution.AssistantToolResponse;
import org.jetbrains.annotations.NotNull;

import static com.dbn.assistant.tool.AssistantToolCache.getUtilityDefinition;

public class AssistantToolInfoProviderImpl extends AssistantStateExtension implements AssistantToolInfoProvider {
    private final AssistantToolInvocation invocation;

    public AssistantToolInfoProviderImpl(@NotNull AssistantState assistantState, AssistantToolInvocation invocation) {
        super(assistantState);
        this.invocation = invocation;
    }

    @Override
    public String getToolName() {
        String toolName = getToolRequest().getToolName();

        AssistantTool tool = getTool();
        AssistantToolInfo.UtilityDefinition definition = getUtilityDefinition(tool, toolName);
        if (definition == null) return toolName;

        return definition.name();
    }

    @Override
    public String getToolDescription() {
        return "";
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
        return "";
    }

    @Override
    public String getToolResponseSummary() {
        return "";
    }

    private AssistantTool getTool() {
        AssistantToolCache toolCache = getToolCache();

        String toolName = getToolRequest().getToolName();
        return toolCache.getAssistantTool(toolName);
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
        AssistantState assistantState = getAssistantState();
        return AssistantToolApprovals.get(assistantState);
    }
}
