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
import com.dbn.assistant.tool.approval.AssistantToolApprovals;
import com.dbn.common.color.Colors;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.common.ui.util.Fonts;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.ui.UIUtil;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.DENIED;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.DISABLED;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.PROMPTED;
import static com.dbn.common.dispose.Failsafe.nn;
import static com.dbn.common.util.Commons.nvl;

public class AssistantToolSettingsTypeForm extends DBNFormBase {
    private JPanel mainPanel;
    private JLabel nameLabel;
    private JLabel infoLabel;
    private DBNComboBox<AssistantToolApprovalStatus> stateComboBox;
    private JTextPane descriptionTextPane;

    private final AssistantToolType type;

    public AssistantToolSettingsTypeForm(AssistantToolSettingsCategoryForm categoryForm, AssistantToolType type) {
        super(categoryForm);
        this.type = type;

        initNameLabel();
        initInfoLabel();
        initDescriptionPanel();
        initStateSelector();

        refreshState();
    }

    private void initDescriptionPanel() {
        descriptionTextPane.setForeground(Colors.faded(UIUtil.getLabelForeground()));
        descriptionTextPane.setText(getAssistantTool().getDescription());
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

    private void initStateSelector() {
        AssistantToolApprovalStatus status = getToolApprovals().getStatus(type);
        stateComboBox.setValues(AssistantToolApprovalStatus.values());
        stateComboBox.setSelectedValue(status);

        ComboBoxes.onSelectionChange(stateComboBox, v -> refreshState());
    }

    private AssistantToolApprovals getToolApprovals() {
        return getCategoryForm().getToolApprovals();
    }
    
    private AssistantToolCache getToolCache() {
        return getCategoryForm().getToolCache();
    }

    private AssistantToolSettingsCategoryForm getCategoryForm() {
        return ensureParentComponent();
    }

    private AssistantTool getAssistantTool() {
        AssistantToolCache toolCache = getToolCache();
        return nn(toolCache.getAssistantTool(type));
    }

    public AssistantToolApprovalStatus getApprovalStatus() {
        return nvl(stateComboBox.getSelectedValue(), PROMPTED);
    }

    public void refreshState() {
        AssistantToolSettingsCategoryForm toolCategoryForm = getCategoryForm();
        AssistantToolApprovalStatus categoryStatus = toolCategoryForm.getApprovalStatus();
        AssistantToolApprovalStatus typeStatus = getApprovalStatus();

        boolean editable = categoryStatus.isOneOf(PROMPTED);
        stateComboBox.setEnabled(editable);
        if (!editable)  {
            stateComboBox.setSelectedValue(PROMPTED);
        }

        boolean enabled =
                !categoryStatus.isOneOf(DENIED, DISABLED) &&
                !typeStatus.isOneOf(DENIED, DISABLED);
        nameLabel.setEnabled(enabled);

        Icon infoIcon = enabled ? AllIcons.General.Note : IconLoader.getDisabledIcon(AllIcons.General.Note);
        //infoLabel.setIcon(infoIcon);
        descriptionTextPane.setForeground(enabled ? Colors.faded(UIUtil.getLabelForeground()): UIUtil.getLabelDisabledForeground());
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
