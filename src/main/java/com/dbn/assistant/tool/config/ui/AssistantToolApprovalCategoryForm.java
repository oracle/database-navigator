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
import com.dbn.assistant.tool.AssistantToolCategory;
import com.dbn.assistant.tool.AssistantToolType;
import com.dbn.assistant.tool.approval.AssistantToolApprovalStatus;
import com.dbn.assistant.tool.approval.AssistantToolApprovals;
import com.dbn.common.color.Colors;
import com.dbn.common.ui.Layouts;
import com.dbn.common.ui.util.Fonts;
import com.dbn.common.util.Actions;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Map;

import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.APPROVED;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.PROMPTED;

public class AssistantToolApprovalCategoryForm extends AssistantToolApprovalItemForm {
    private JPanel mainPanel;
    private JLabel nameLabel;
    private JLabel infoLabel;
    private JPanel toolTypesPanel;
    private JTextPane descriptionTextPane;
    private JPanel actionsPanel;

    private final AssistantToolCategory category;
    private final Map<AssistantToolType, AssistantToolApprovalTypeForm> toolTypeForms = ContainerUtil.createConcurrentWeakValueMap();

    public AssistantToolApprovalCategoryForm(AssistantToolApprovalForm settingsForm, AssistantToolCategory category) {
        super(settingsForm);
        this.category = category;

        initNameLabel();
        initInfoLabel();
        initDescriptionPanel();
        initActionsPanel();
        initToolTypesPanel();
    }

    private void initNameLabel() {
        nameLabel.setFont(Fonts.regular(2));
        nameLabel.setText(category.getName());
    }

    private void initInfoLabel() {
        infoLabel.setIcon(AllIcons.General.Note);
        infoLabel.setText("");
        infoLabel.setToolTipText(category.getDescription());
    }

    private void initDescriptionPanel() {
        descriptionTextPane.setForeground(Colors.faded(UIUtil.getLabelForeground()));
        descriptionTextPane.setText(category.getDescription());
        descriptionTextPane.setVisible(false);
    }

    private void initActionsPanel() {
        ActionToolbar chatActions = Actions.createActionToolbar(actionsPanel, true, "DBNavigator.ActionGroup.AssistantToolApprovalActions");
        JComponent component = chatActions.getComponent();
        component.setOpaque(false);
        this.actionsPanel.add(component, BorderLayout.NORTH);
    }

    private void initToolTypesPanel() {
        Layouts.verticalBoxLayout(toolTypesPanel);

        AssistantToolCache toolCache = getToolCache();
        List<AssistantToolType> toolTypes =  toolCache.getToolTypes(category);
        for (AssistantToolType toolType : toolTypes) {
            AssistantToolApprovalTypeForm toolTypeForm = new AssistantToolApprovalTypeForm(this, toolType);
            toolTypeForms.put(toolType, toolTypeForm);
            toolTypesPanel.add(toolTypeForm.getComponent());
        }
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public AssistantToolApprovalStatus getApprovalStatus() {
        return getToolApprovals().getStatus(category);
    }

    @Override
    public void setApprovalStatus(AssistantToolApprovalStatus status) {
        AssistantToolApprovals approvals = getToolApprovals();
        approvals.setStatus(category, status);

        List<AssistantToolType> types = getToolCache().getToolTypes(category);
        for (AssistantToolType type : types) {
            approvals.setStatus(type, status);
        }

        refreshState();
        updateActionToolbars();
    }

    public void refreshState() {
        AssistantToolApprovalStatus categoryStatus = getApprovalStatus();
        getToolApprovals().setStatus(category, categoryStatus);
        boolean enabled = categoryStatus.isOneOf(PROMPTED, APPROVED);

        nameLabel.setEnabled(enabled);

        Icon infoIcon = enabled ? AllIcons.General.Note : IconLoader.getDisabledIcon(AllIcons.General.Note);
        infoLabel.setIcon(infoIcon);

        descriptionTextPane.setForeground(enabled ?
                Colors.faded(UIUtil.getLabelForeground()):
                UIUtil.getLabelDisabledForeground());

        for (AssistantToolApprovalTypeForm toolTypeForm : toolTypeForms.values()) {
            toolTypeForm.refreshState();
        }
    }
}
