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

package com.dbn.assistant.mcp.ui;

import com.dbn.assistant.mcp.AssistantMcpToolApprovals;
import com.dbn.assistant.mcp.AssistantMcpToolInfo;
import com.dbn.assistant.tool.approval.AssistantToolApprovalStatus;
import com.dbn.common.color.Colors;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNToggleButton;
import com.dbn.common.ui.util.Fonts;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.APPROVED;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.PROMPTED;
import static com.dbn.common.ui.misc.DBNToggleButton.getDefaultForeground;
import static com.dbn.common.ui.misc.DBNToggleButton.getErrorForeground;
import static com.dbn.common.ui.misc.DBNToggleButton.getSuccessForeground;

public class AssistantMcpToolApprovalForm extends DBNFormBase {
    private JPanel mainPanel;
    private JLabel nameLabel;
    private JTextPane descriptionTextPane;
    private DBNToggleButton<AssistantToolApprovalStatus> statusToggle;

    private final @Getter AssistantMcpToolInfo toolInfo;

    public AssistantMcpToolApprovalForm(AssistantMcpToolApprovalsForm approvalsForm, AssistantMcpToolInfo toolInfo) {
        super(approvalsForm);
        this.toolInfo = toolInfo;


        initNameLabel();
        initDescriptionPanel();
        initStatusToggle();
    }

    private void initStatusToggle() {
        statusToggle.setTextColor(s ->
                switch (s) {
                    case PROMPTED -> getDefaultForeground();
                    case APPROVED -> getSuccessForeground();
                    case BLOCKED -> getErrorForeground();
                });
        AssistantToolApprovalStatus[] approvalStatuses = AssistantToolApprovalStatus.values();

        statusToggle.setValues(approvalStatuses);
        statusToggle.setSelectedValue(getToolApproval());
        statusToggle.addListener((os, ns) -> setApprovalStatus(ns));
    }

    private void initNameLabel() {
        nameLabel.setFont(Fonts.regular(2));
        nameLabel.setText(toolInfo.getName());
    }

    private void initDescriptionPanel() {
        descriptionTextPane.setForeground(Colors.faded(UIUtil.getLabelForeground()));
        descriptionTextPane.setText(toolInfo.getDescription());
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public AssistantMcpToolApprovals getToolApprovals() {
        AssistantMcpToolApprovalsForm approvalsForm = ensureParentComponent();
        return approvalsForm.getToolApprovals();
    }

    public void setApprovalStatus(AssistantToolApprovalStatus status) {
        AssistantMcpToolApprovals approvals = getToolApprovals();
        approvals.setStatus(toolInfo.getServerId(), toolInfo.getName(), status);

        refreshState();
        updateActionToolbars();
    }

    public void refreshState() {
        AssistantToolApprovalStatus categoryStatus = getToolApproval();
        statusToggle.setSelectedValue(categoryStatus);

        boolean enabled = categoryStatus.isOneOf(PROMPTED, APPROVED);
        nameLabel.setEnabled(enabled);

        descriptionTextPane.setForeground(enabled ?
                Colors.faded(UIUtil.getLabelForeground()) :
                UIUtil.getLabelDisabledForeground());
    }

    private AssistantToolApprovalStatus getToolApproval() {
        return getToolApprovals().getStatus(toolInfo.getServerId(), toolInfo.getName());
    }

    public void resetState() {
        AssistantToolApprovalStatus categoryStatus = getToolApproval();
        statusToggle.setSelectedValueSilently(categoryStatus);
    }
}
