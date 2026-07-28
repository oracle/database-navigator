/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.liquibase.workspace.ui;

import com.dbn.common.environment.EnvironmentType;
import com.dbn.common.environment.EnvironmentTypeId;
import com.dbn.common.environment.options.EnvironmentSettings;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.liquibase.workspace.LiquibaseEnvironmentProfile;
import com.dbn.liquibase.workspace.LiquibaseEnvironmentProfileBundle;
import com.dbn.options.general.GeneralProjectSettings;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.ui.form.field.DBNFormFieldDisabler.setFormFieldEnabled;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.nls.NlsResources.txt;

/** Detail form for a named Liquibase environment profile. */
public class LiquibaseEnvironmentProfileForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel hintPanel;
    private JBTextField nameTextField;
    private DBNComboBox<EnvironmentType> environmentTypeSelector;
    private JBTextField contextsTextField;
    private JBTextField labelsTextField;
    private JCheckBox requireSqlPreviewCheckBox;
    private JCheckBox allowDestructiveOperationsCheckBox;
    private JCheckBox requireConfirmationCheckBox;

    private final LiquibaseEnvironmentProfile profile;
    private final LiquibaseEnvironmentProfileBundle bundle;
    private final boolean environmentTypeEditable;

    LiquibaseEnvironmentProfileForm(
            @NotNull DBNComponent parent,
            @NotNull LiquibaseEnvironmentProfileBundle bundle,
            @NotNull LiquibaseEnvironmentProfile profile,
            boolean environmentTypeEditable) {
        super(parent);
        this.profile = profile;
        this.bundle = bundle;
        this.environmentTypeEditable = environmentTypeEditable;
        initHintPanel();
        initEnvironmentTypes();
        onTextChange(nameTextField, e -> updateProfileName());
        resetFormChanges();
    }

    private void initHintPanel() {
        hintPanel.add(new DBNHintForm(
                this,
                TextContent.plain(txt("app.liquibase.hint.EnvironmentProfile")),
                null,
                true).getComponent());
    }

    private void updateProfileName() {
        profile.setName(getText(nameTextField));

        DBNComponent parent = ensureParentComponent();
        if (parent instanceof LiquibaseEnvironmentProfilesForm profilesForm) {
            profilesForm.refreshProfileList();
        }
    }

    private void initEnvironmentTypes() {
        EnvironmentSettings settings = GeneralProjectSettings.getInstance(ensureProject()).getEnvironmentSettings();
        List<EnvironmentType> types = new ArrayList<>();
        types.add(EnvironmentType.DEFAULT);
        types.addAll(settings.getEnvironmentTypes().getEnvironmentTypes());
        environmentTypeSelector.setValues(types);
        setFormFieldEnabled(environmentTypeSelector, "CONTEXT_AVAILABILITY", environmentTypeEditable);
    }

    @Override
    protected void initValidation() {
        addRequiredTextValidation(nameTextField, txt("msg.liquibase.error.EnvironmentProfileNameRequired"));
        addValidation(nameTextField, field -> validateProfileName());
        addSelectionValidation(environmentTypeSelector, txt("msg.liquibase.error.EnvironmentTypeRequired"));
    }

    private String validateProfileName() {
        return bundle.findNameOwner(getText(nameTextField), profile) == null ?
                null : txt("msg.liquibase.error.EnvironmentProfileNameAlreadyUsed");
    }

    @Override
    public void resetFormChanges() {
        setText(nameTextField, profile.getName());
        setSelection(environmentTypeSelector, getEnvironmentType(profile.getEnvironmentTypeId()));
        setText(contextsTextField, profile.getContexts());
        setText(labelsTextField, profile.getLabels());
        requireSqlPreviewCheckBox.setSelected(profile.isRequireSqlPreview());
        allowDestructiveOperationsCheckBox.setSelected(profile.isAllowDestructiveOperations());
        requireConfirmationCheckBox.setSelected(profile.isRequireConfirmation());
    }

    @Override
    public void applyFormChanges() {
        profile.setName(getText(nameTextField));
        EnvironmentType environmentType = getSelection(environmentTypeSelector);
        profile.setEnvironmentTypeId(environmentType == null ? EnvironmentTypeId.DEFAULT : environmentType.getId());
        profile.setContexts(getText(contextsTextField));
        profile.setLabels(getText(labelsTextField));
        profile.setRequireSqlPreview(requireSqlPreviewCheckBox.isSelected());
        profile.setAllowDestructiveOperations(allowDestructiveOperationsCheckBox.isSelected());
        profile.setRequireConfirmation(requireConfirmationCheckBox.isSelected());
    }

    private EnvironmentType getEnvironmentType(EnvironmentTypeId id) {
        EnvironmentSettings settings = GeneralProjectSettings.getInstance(ensureProject()).getEnvironmentSettings();
        return id == EnvironmentTypeId.DEFAULT ? EnvironmentType.DEFAULT : settings.getEnvironmentType(id);
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return nameTextField;
    }

    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
