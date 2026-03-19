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

import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.tool.AssistantToolCategory;
import com.dbn.assistant.tool.AssistantToolType;
import com.dbn.assistant.tool.approval.AssistantToolApprovalStatus;
import com.dbn.assistant.tool.approval.AssistantToolApprovals;
import com.dbn.assistant.tool.config.AssistantToolSettings;
import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.Layouts;
import com.dbn.common.ui.info.DBNInfoLabel;
import com.dbn.common.ui.misc.DBNToggleButton;
import com.dbn.common.ui.util.Fonts;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.util.List;
import java.util.Map;

import static com.dbn.assistant.tool.AssistantToolCategory.USER_INTERACTION;
import static com.dbn.assistant.tool.AssistantToolData.getSupportedToolTypes;
import static com.dbn.assistant.tool.AssistantToolData.getToolTypes;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.APPROVED;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.BLOCKED;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.PROMPTED;
import static com.dbn.common.constant.Constant.array;
import static com.dbn.common.ui.misc.DBNToggleButton.getDefaultForeground;
import static com.dbn.common.ui.misc.DBNToggleButton.getErrorForeground;
import static com.dbn.common.ui.misc.DBNToggleButton.getSuccessForeground;

public class AssistantToolApprovalCategoryForm extends AssistantToolApprovalItemForm {
    private JPanel mainPanel;
    private JLabel nameLabel;
    private DBNInfoLabel infoLabel;
    private JPanel toolTypesPanel;
    private JTextPane descriptionTextPane;
    private DBNToggleButton<AssistantToolApprovalStatus> statusToggle;

    private final AssistantToolCategory category;
    private final Map<AssistantToolType, AssistantToolApprovalTypeForm> toolTypeForms = ContainerUtil.createConcurrentWeakValueMap();

    public AssistantToolApprovalCategoryForm(AssistantToolApprovalForm settingsForm, AssistantToolCategory category) {
        super(settingsForm);
        this.category = category;


        initNameLabel();
        initInfoLabel();
        initDescriptionPanel();
        initToolTypesPanel();
        initStatusToggle();
    }

    private void initStatusToggle() {
        statusToggle.setTextColor(s ->
                switch (s) {
                    case PROMPTED -> getDefaultForeground();
                    case APPROVED -> getSuccessForeground();
                    case BLOCKED -> getErrorForeground();
                });
        AssistantToolApprovalStatus[] approvalStatuses = category == USER_INTERACTION ?
                array(PROMPTED, BLOCKED) : // interactive tools are always prompted; cannot be pre-approved
                AssistantToolApprovalStatus.values();

        statusToggle.setValues(approvalStatuses);
        statusToggle.setSelectedValue(getApprovalStatus());
        statusToggle.addListener((os, ns) -> setApprovalStatus(ns));
    }

    private void initNameLabel() {
        nameLabel.setFont(Fonts.regular(2));
        nameLabel.setText(category.getName());
    }

    private void initInfoLabel() {
        infoLabel.setContent(TextContent.plain(category.getDescription()));
    }

    private void initDescriptionPanel() {
        descriptionTextPane.setForeground(Colors.faded(UIUtil.getLabelForeground()));
        descriptionTextPane.setText(category.getDescription());
        descriptionTextPane.setVisible(false); // TODO overcrowds the tool approval screens
    }

    private void initToolTypesPanel() {
        Layouts.verticalBoxLayout(toolTypesPanel);
        AssistantState assistantState = getAssistantState();

        List<AssistantToolType> toolTypes = getSupportedToolTypes(assistantState, category);
        for (AssistantToolType toolType : toolTypes) {
            AssistantToolApprovalTypeForm toolTypeForm = new AssistantToolApprovalTypeForm(this, toolType);
            toolTypeForms.put(toolType, toolTypeForm);
            toolTypesPanel.add(toolTypeForm.getComponent());
        }
    }

    private AssistantState getAssistantState() {
        AssistantToolApprovalForm approvalForm = ensureParentComponent();
        AssistantToolSettings settings = approvalForm.getSettings();
        return settings.getAssistantState();
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

        List<AssistantToolType> types = getToolTypes(category);
        for (AssistantToolType type : types) {
            approvals.setStatus(type, status);
        }

        refreshState();
        updateActionToolbars();
    }

    public void refreshState() {
        AssistantToolApprovalStatus categoryStatus = getApprovalStatus();
        statusToggle.setSelectedValue(categoryStatus);

        boolean enabled = categoryStatus.isOneOf(PROMPTED, APPROVED);
        nameLabel.setEnabled(enabled);

        Icon infoIcon = enabled ? Icons.ACTION_INFO : IconLoader.getDisabledIcon(Icons.ACTION_INFO);
        infoLabel.setIcon(infoIcon);

        descriptionTextPane.setForeground(enabled ?
                Colors.faded(UIUtil.getLabelForeground()) :
                UIUtil.getLabelDisabledForeground());

        for (AssistantToolApprovalTypeForm toolTypeForm : toolTypeForms.values()) {
            toolTypeForm.refreshState();
        }
    }

    public void resetState() {
        AssistantToolApprovalStatus categoryStatus = getApprovalStatus();
        statusToggle.setSelectedValueSilently(categoryStatus);
    }
}
