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

import com.dbn.assistant.tool.AssistantTool;
import com.dbn.assistant.tool.AssistantToolCache;
import com.dbn.assistant.tool.AssistantToolType;
import com.dbn.assistant.tool.approval.AssistantToolApprovalStatus;
import com.dbn.common.color.Colors;
import com.dbn.common.ui.util.Fonts;
import com.dbn.common.util.Actions;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.util.ui.UIUtil;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.BorderLayout;

import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.APPROVED;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.PROMPTED;
import static com.dbn.common.dispose.Failsafe.nn;

public class AssistantToolApprovalTypeForm extends AssistantToolApprovalItemForm {
    private JPanel mainPanel;
    private JLabel nameLabel;
    private JTextPane descriptionTextPane;
    private JPanel actionsPanel;

    private final AssistantToolType type;

    public AssistantToolApprovalTypeForm(AssistantToolApprovalCategoryForm categoryForm, AssistantToolType type) {
        super(categoryForm);
        this.type = type;

        initNameLabel();
        initInfoLabel();
        initDescriptionPanel();
        initActionsPanel();

        refreshState();
    }

    private void initNameLabel() {
        AssistantTool tool = getAssistantTool();
        nameLabel.setText(tool.getName());
        nameLabel.setFont(Fonts.regular(1));
    }

    private void initInfoLabel() {
/*        AssistantTool tool = getAssistantTool();
        infoLabel.setIcon(AllIcons.General.Note);
        infoLabel.setText("");
        infoLabel.setToolTipText(tool.getDescription());*/
    }

    private void initDescriptionPanel() {
        descriptionTextPane.setForeground(Colors.faded(UIUtil.getLabelForeground()));
        descriptionTextPane.setText(getAssistantTool().getDescription());
    }

    private void initActionsPanel() {
        ActionToolbar chatActions = Actions.createActionToolbar(actionsPanel, true, "DBNavigator.ActionGroup.AssistantToolApprovalActions");
        JComponent component = chatActions.getComponent();
        component.setOpaque(false);
        this.actionsPanel.add(component, BorderLayout.NORTH);
    }

    private AssistantToolApprovalCategoryForm getCategoryForm() {
        return ensureParentComponent();
    }

    private AssistantTool getAssistantTool() {
        AssistantToolCache toolCache = getToolCache();
        return nn(toolCache.getAssistantTool(type));
    }

    public AssistantToolApprovalStatus getApprovalStatus() {
        return getToolApprovals().getStatus(type);
    }

    @Override
    public AssistantToolApprovalStatus getParentApprovalStatus() {
        AssistantTool tool = getAssistantTool();
        return getToolApprovals().getStatus(tool.getCategory());
    }

    @Override
    public void setApprovalStatus(AssistantToolApprovalStatus status) {
        getToolApprovals().setStatus(type, status);
        refreshState();
    }

    public void refreshState() {
        AssistantToolApprovalCategoryForm toolCategoryForm = getCategoryForm();
        AssistantToolApprovalStatus categoryStatus = toolCategoryForm.getApprovalStatus();
        AssistantToolApprovalStatus typeStatus = getApprovalStatus();

        boolean enabled =
                categoryStatus.isOneOf(PROMPTED, APPROVED) &&
                typeStatus.isOneOf(PROMPTED, APPROVED);
        nameLabel.setEnabled(enabled);

        descriptionTextPane.setForeground(enabled ?
                Colors.faded(UIUtil.getLabelForeground()):
                UIUtil.getLabelDisabledForeground());
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
