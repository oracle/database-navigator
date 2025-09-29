/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.assistant.profile.ui;

import com.dbn.assistant.AssistantType;
import com.dbn.assistant.credential.AssistantCredential;
import com.dbn.assistant.profile.AssistantTemperaturePreset;
import com.dbn.assistant.profile.DeclaredAssistantProfile;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.provider.AIProviderData;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.common.util.Lists;
import com.dbn.common.util.Strings;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import static com.dbn.assistant.profile.AssistantTemperaturePreset.BALANCED;
import static com.dbn.assistant.profile.AssistantTemperaturePreset.CUSTOM;
import static com.dbn.assistant.profile.AssistantTemperaturePreset.values;
import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.ui.util.TextFields.setTextSilently;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Naming.nextNumberedIdentifier;

public class AssistantProfileEditForm extends DBNFormBase {
    private JPanel headerPanel;
    private JPanel mainPanel;
    private JBTextField nameTextField;
    private DBNComboBox<AIProvider> providerComboBox;
    private DBNComboBox<AssistantCredential> credentialComboBox;
    private DBNComboBox<AssistantTemperaturePreset> temperatureComboBox;
    private JSlider temperatureSlider;
    private JPanel temperatureDescPanel;
    private JBTextArea instructionsTextArea;


    private final DeclaredAssistantProfile profile;
    private final Set<String> usedNames;
    private boolean generatedName;

    private DBNHintForm temperatureDescForm;

    AssistantProfileEditForm(AssistantProfileEditDialog parent, Set<String> usedNames) {
        super(parent);
        this.profile = parent.getProfile();
        this.usedNames = usedNames;
        this.generatedName = Strings.isEmpty(profile.getName());

        initComboBox(providerComboBox, getProviders());
        initComboBox(credentialComboBox, getCredentials());
        initComboBox(temperatureComboBox, values());
        initTemperatureFields();

        instructionsTextArea.getEmptyText().setText("e.g. ‘Use Java best practices’ or ‘Comment each step.’");

        resetFormChanges();

        updateFields();
        onSelectionChange(providerComboBox, c -> updateFields());
        onSelectionChange(temperatureComboBox, c -> updateFields());
        onTextChange(nameTextField, e -> generatedName = false);
    }

    private void initTemperatureFields() {
        temperatureComboBox.set(HIDE_DESCRIPTION, true);
        temperatureDescForm = new DBNHintForm(this, null, null, true);
        temperatureDescPanel.add(temperatureDescForm.getMainComponent());
        temperatureSlider.addChangeListener(e -> updateSliderLabels());
    }


    private void updateSliderLabels() {
        int currentValue = temperatureSlider.getValue();
        Hashtable<Integer, JLabel> labels = new Hashtable<>();
        labels.put(0, new JLabel(currentValue > 5 ? "0" : ""));
        labels.put(currentValue, new JLabel(String.valueOf((float) currentValue / 100)));
        labels.put(100, new JLabel(currentValue < 95 ? "1" : ""));
        temperatureSlider.setLabelTable(labels);
    }

    private void updateFields() {
        initProfileName();
        initComboBox(credentialComboBox, getCredentials());
        temperatureSlider.setVisible(isCustomTemperature());
        temperatureDescForm.setHintContent(TextContent.plain(getSelectedTemperature().getDescription()));
    }

    private void initProfileName() {
        if (!generatedName) return;

        AIProvider provider = getSelectedProvider();
        String baseName = provider == null ? "Profile" : provider.getName();

        String name = nextNumberedIdentifier(baseName + " 1", true, () -> usedNames);
        setTextSilently(nameTextField, name);
    }

    private List<AssistantCredential> getCredentials() {
        AssistantProfileEditDialog parent = getParentDialog();
        if (parent == null) return Collections.emptyList();

        String selectedProviderId = getSelectedProviderId();
        return Lists.filter(parent.getCredentials(), c ->
                c.getProviderId() == null ||
                selectedProviderId == null ||
                c.getProviderId().equals(selectedProviderId));
    }

    private AssistantCredential getCredential(String id) {
        return Lists.first(getCredentials(), c -> c.getId().equals(id));
    }


    private static List<AIProvider> getProviders() {
        return AIProviderData.getProviders(AssistantType.PUBLIC);
    }

    private String getSelectedProviderId() {
        AIProvider provider = getSelectedProvider();
        return provider == null ? null : provider.getId();
    }

    private @Nullable AIProvider getSelectedProvider() {
        return ComboBoxes.getSelection(providerComboBox);
    }

    private String getSelectedCredentialId() {
        AssistantCredential credential = ComboBoxes.getSelection(credentialComboBox);
        return credential == null ? null : credential.getId();
    }

    @Override
    protected void initValidation() {
        addTextValidation(nameTextField, Strings::isNotEmpty, "Please provide a profile name");
        addTextValidation(nameTextField, this::isNotUsed, "The profile name is already in use");
    }

    public void applyFormChanges() {
        profile.setName(getText(nameTextField));
        profile.setProviderId(getSelectedProviderId());
        profile.setCredentialId(getSelectedCredentialId());
        profile.setTemperaturePreset(getSelectedTemperature());
        profile.setTemperature(isCustomTemperature() ? temperatureSlider.getValue() / 100.0 : getSelectedTemperature().getValue());
    }

    private AssistantTemperaturePreset getSelectedTemperature() {
        return nvl(getSelection(temperatureComboBox), BALANCED);
    }

    private boolean isCustomTemperature() {
        return getSelectedTemperature() == CUSTOM;
    }

    public void resetFormChanges() {
        nameTextField.setText(profile.getName());

        AIProvider provider = AIProviderData.getProvider(AssistantType.PUBLIC, profile.getProviderId());
        setSelection(providerComboBox, provider);

        AssistantCredential credential = getCredential(profile.getCredentialId());
        setSelection(credentialComboBox, credential);

        setSelection(temperatureComboBox, profile.getTemperaturePreset());
        temperatureSlider.setValue((int) (profile.getTemperature() * 100));
    }

    private boolean isNotUsed(String name) {
        return !usedNames.contains(name);
    }

    public String getProfileName() {
        return getText(nameTextField);
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return nameTextField;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

}
