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

package com.dbn.assistant.tool.config;

import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.state.AssistantStateExtension;
import com.dbn.assistant.tool.approval.AssistantToolApprovals;
import com.dbn.common.action.UserDataKeys;
import com.dbn.common.state.PersistentStateElement;
import lombok.Getter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.options.setting.Settings.newElement;


@Getter
public class AssistantToolSettings extends AssistantStateExtension implements PersistentStateElement {
    private final AssistantToolApprovals toolApprovals = new AssistantToolApprovals();

    protected AssistantToolSettings(@NotNull AssistantState assistantState) {
        super(assistantState);
    }

    public static AssistantToolSettings get(AssistantState assistantState) {
        return UserDataKeys.getUserDataSync(assistantState, UserDataKeys.ASSISTANT_TOOL_SETTINGS, () -> new AssistantToolSettings(assistantState));
    }

    @Override
    public void readState(Element element) {
        if (element == null) return;

        Element approvalsElement = element.getChild("approvals");
        toolApprovals.readState(approvalsElement);
    }

    @Override
    public void writeState(Element element) {
        if (element == null) return;

        if (!toolApprovals.isEmpty()) {
            Element approvalsElement = newElement(element, "approvals");
            toolApprovals.writeState(approvalsElement);
        }
    }
}
