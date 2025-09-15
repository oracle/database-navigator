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
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.common.ui.util.Fonts;
import com.intellij.icons.AllIcons;
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

import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.DENIED;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.DISABLED;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.PROMPTED;
import static com.dbn.common.util.Commons.nvl;

public class AssistantToolSettingsCategoryForm extends DBNFormBase {
    private JPanel mainPanel;
    private JLabel nameLabel;
    private JLabel infoLabel;
    private DBNComboBox<AssistantToolApprovalStatus> stateComboBox;
    private JPanel toolTypesPanel;
    private JTextPane descriptionTextPane;

    private final AssistantToolCategory category;
    private final Map<AssistantToolType, AssistantToolSettingsTypeForm> toolTypeForms = ContainerUtil.createConcurrentWeakValueMap();

    public AssistantToolSettingsCategoryForm(AssistantToolSettingsForm settingsForm, AssistantToolCategory category) {
        super(settingsForm);
        this.category = category;

        initNameLabel();
        initInfoLabel();
        initDescriptionPanel();
        initStateSelector();
        initToolTypesPanel();
    }

    private void initDescriptionPanel() {
        descriptionTextPane.setForeground(Colors.faded(UIUtil.getLabelForeground()));
        descriptionTextPane.setText(category.getDescription());
        descriptionTextPane.setVisible(false);
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

    private void initStateSelector() {
        AssistantToolApprovalStatus status = getToolApprovals().getStatus(category);

        stateComboBox.setValues(AssistantToolApprovalStatus.values());
        stateComboBox.setSelectedValue(status);
        ComboBoxes.onSelectionChange(stateComboBox, v -> refreshState());
    }

    private void initToolTypesPanel() {
        Layouts.verticalBoxLayout(toolTypesPanel);

        AssistantToolCache toolCache = getToolCache();
        List<AssistantToolType> toolTypes =  toolCache.getToolTypes(category);
        for (AssistantToolType toolType : toolTypes) {
            AssistantToolSettingsTypeForm toolTypeForm = new AssistantToolSettingsTypeForm(this, toolType);
            toolTypeForms.put(toolType, toolTypeForm);
            toolTypesPanel.add(toolTypeForm.getMainComponent());
        }
    }

    protected AssistantToolApprovals getToolApprovals() {
        AssistantToolSettingsForm settingsForm = ensureParentComponent();
        return settingsForm.getToolApprovals();
    }

    protected AssistantToolCache getToolCache() {
        AssistantToolSettingsForm settingsForm = ensureParentComponent();
        return settingsForm.getToolCache();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public AssistantToolApprovalStatus getApprovalStatus() {
        return nvl(stateComboBox.getSelectedValue(), PROMPTED);
    }

    public void refreshState() {
        AssistantToolApprovalStatus categoryStatus = getApprovalStatus();
        getToolApprovals().setStatus(category, categoryStatus);
        boolean enabled = !categoryStatus.isOneOf(DENIED, DISABLED);
        nameLabel.setEnabled(enabled);

        Icon infoIcon = enabled ? AllIcons.General.Note : IconLoader.getDisabledIcon(AllIcons.General.Note);
        infoLabel.setIcon(infoIcon);

        descriptionTextPane.setForeground(enabled ? Colors.faded(UIUtil.getLabelForeground()): UIUtil.getLabelDisabledForeground());
        for (AssistantToolSettingsTypeForm toolTypeForm : toolTypeForms.values()) {
            toolTypeForm.refreshState();
        }
    }
}
