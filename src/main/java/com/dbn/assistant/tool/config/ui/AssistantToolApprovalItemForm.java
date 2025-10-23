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

package com.dbn.assistant.tool.config.ui;

import com.dbn.assistant.tool.AssistantToolCache;
import com.dbn.assistant.tool.approval.AssistantToolApprovalStatus;
import com.dbn.assistant.tool.approval.AssistantToolApprovals;
import com.dbn.common.action.DataKeys;
import com.dbn.common.ui.form.DBNFormBase;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AssistantToolApprovalItemForm extends DBNFormBase {
    public AssistantToolApprovalItemForm(@Nullable Disposable parent) {
        super(parent);
    }

    public AssistantToolApprovalStatus getParentApprovalStatus() {
        return null;
    }

    public abstract AssistantToolApprovalStatus getApprovalStatus();

    public abstract void setApprovalStatus(AssistantToolApprovalStatus status);

    protected final AssistantToolApprovals getToolApprovals() {
        AssistantToolApprovalForm settingsForm = getSettingsForm();
        return settingsForm.getToolApprovals();
    }

    protected final AssistantToolCache getToolCache() {
        AssistantToolApprovalForm settingsForm = getSettingsForm();
        return settingsForm.getToolCache();
    }

    private @NotNull AssistantToolApprovalForm getSettingsForm() {
        return ensureParentFrom(AssistantToolApprovalForm.class);
    }


    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.ASSISTANT_TOOL_APPROVAL_FORM.is(dataId)) return this;
        return null;
    }


}
