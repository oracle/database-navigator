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
import com.dbn.assistant.credential.AssistantCredentialBundle;
import com.dbn.assistant.credential.ui.AssistantCredentialEditDialog;
import com.dbn.assistant.credential.ui.AssistantCredentialEditRequest;
import com.dbn.assistant.profile.AssistantTemperaturePreset;
import com.dbn.assistant.profile.DeclaredAssistantProfile;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.provider.AIProviderData;
import com.dbn.assistant.provider.AIProviderId;
import com.dbn.common.routine.Consumer;
import com.dbn.common.text.TextContent;
import com.dbn.common.text.TextResources;
import com.dbn.common.ui.ValueFactory;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.info.DBNInfoLabel;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Dialogs;
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
import java.util.Hashtable;
import java.util.List;

import static com.dbn.assistant.profile.AssistantTemperaturePreset.BALANCED;
import static com.dbn.assistant.profile.AssistantTemperaturePreset.CUSTOM;
import static com.dbn.assistant.profile.AssistantTemperaturePreset.values;
import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
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
    private JPanel hintPanel;
    private JPanel mainPanel;
    private JBTextField nameTextField;
    private DBNComboBox<AIProvider> providerComboBox;
    private DBNComboBox<AssistantCredential> credentialComboBox;
    private DBNComboBox<AssistantTemperaturePreset> temperatureComboBox;
    private JSlider temperatureSlider;
    private JBTextArea instructionsTextArea;
    private DBNInfoLabel temperatureInfoLabel;


    private final DeclaredAssistantProfile profile;
    private boolean generatedName;

    AssistantProfileEditForm(AssistantProfileEditDialog parent) {
        super(parent);
        this.profile = parent.getProfile();
        this.generatedName = Strings.isEmpty(profile.getName());

        initComboBox(providerComboBox, getProviders());
        initHintPanel();
        initCredentialFields();
        initTemperatureFields();
        initInstructionsField();

        resetFormChanges();

        updateFields();
        onSelectionChange(providerComboBox, c -> updateFields());
        onSelectionChange(temperatureComboBox, c -> updateFields());
        onTextChange(nameTextField, e -> generatedName = false);
    }

    private void initHintPanel() {
        TextContent hintContent = TextContent.plain(
                "Profiles let you customize your experience with the LLM. " +
                    "You can choose from different temperature presets to adjust the balance between accuracy and creativity. " +
                    "You can also specify custom instructions to help the LLM better understand your needs.");
        DBNHintForm hintForm = new DBNHintForm(this, hintContent, null, true);
        hintPanel.add(hintForm.getComponent());
    }

    private void initInstructionsField() {
        instructionsTextArea.getEmptyText().setText("e.g. ‘Use Java best practices’ or ‘Comment each step.’");
    }

    private void initCredentialFields() {
        initComboBox(credentialComboBox, getFilteredCredentials());
        credentialComboBox.withValueFactory(createCredentialFactory());
    }

    private ValueFactory<AssistantCredential> createCredentialFactory() {
        return new ValueFactory<>("New Credential...") {
            @Override
            public void createValue(Consumer<AssistantCredential> consumer) {
                AssistantCredentialEditRequest request = createNewCredentialRequest(consumer);
                Dialogs.show(() -> new AssistantCredentialEditDialog(getProject(), request));
            }
        };
    }

    private AssistantCredentialEditRequest createNewCredentialRequest(Consumer<AssistantCredential> consumer) {
        AssistantCredentialBundle credentials = getCredentials();
        return AssistantCredentialEditRequest
                .builder()
                .providerId(getSelectedProviderId())
                .credentials(credentials)
                .saveConsumer(c -> {
                    credentials.addCredential(c);
                    consumer.accept(c);
                })
                .build();
    }

    private void initTemperatureFields() {
        initComboBox(temperatureComboBox, values());
        temperatureComboBox.set(HIDE_DESCRIPTION, true);
        temperatureSlider.addChangeListener(e -> updateSliderLabels());

        TextContent infoContent = buildTemperatureInfo();
        temperatureInfoLabel.setContent(infoContent);
    }

    private TextContent buildTemperatureInfo() {
        String infoRawContent = TextResources.get(getClass(), "llm_temperature_info.html.ft");
        TextContent infoContent = TextContent.html(infoRawContent);

        StringBuilder body = new StringBuilder();
        for (AssistantTemperaturePreset value : values()) {
            body.append("\n<u>");
            body.append(value.getName());
            body.append("</u><br>");
            body.append(value.getDescription());
            body.append("<br><br>");
        }

        infoContent.initFonts();
        infoContent.initField("BODY_CONTENT", body.toString());

        return infoContent;
    }

    private void updateSliderLabels() {
        int currentValue = temperatureSlider.getValue();
        Hashtable<Integer, JLabel> labels = new Hashtable<>();
        labels.put(0, new JLabel(currentValue > 5 ? "0" : ""));
        labels.put(currentValue, new JLabel(String.valueOf((float) currentValue / 100)));
        labels.put(100, new JLabel(currentValue < 95 ? "1" : ""));
        temperatureSlider.setLabelTable(labels);
    }

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsAvailability(() -> isProviderSwitchAllowed(), array(providerComboBox));
    }

    private boolean isProviderSwitchAllowed() {
        AssistantProfileEditRequest request = getRequest();
        return request.isNewProfile() && request.getProviderId() == null;
    }


    private void updateFields() {
        initProfileName();
        initComboBox(credentialComboBox, getFilteredCredentials());
        temperatureSlider.setVisible(isCustomTemperature());
    }

    private void initProfileName() {
        if (!generatedName) return;

        AIProvider provider = getSelectedProvider();
        String baseName = provider == null ? "Profile" : provider.getName();

        String name = nextNumberedIdentifier(baseName + " 1", true, () -> getRequest().getUsedNames());
        setTextSilently(nameTextField, name);
    }

    private List<AssistantCredential> getFilteredCredentials() {
        AssistantCredentialBundle credentials = getCredentials();

        AIProviderId selectedProviderId = getSelectedProviderId();
        return Lists.filter(credentials.getElements(), c -> Commons.match(c.getProviderId(), selectedProviderId));
    }

    private AssistantProfileEditRequest getRequest() {
        AssistantProfileEditDialog parent = ensureParentComponent();
        return parent.getRequest();
    }

    private AssistantCredentialBundle getCredentials() {
        return getRequest().getCredentials();
    }

    private AssistantCredential getCredential(String id) {
        return Lists.first(getFilteredCredentials(), c -> c.getId().equals(id));
    }

    private static List<AIProvider> getProviders() {
        return AIProviderData.getProviders(AssistantType.PUBLIC);
    }

    private AIProviderId getSelectedProviderId() {
        AIProvider provider = getSelectedProvider();
        return provider == null ? profile.getProviderId() : provider.getId();
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
        addTextValidation(nameTextField, Strings::isNotEmpty, txt("msg.assistant.error.ProfileNameRequired"));
        addTextValidation(nameTextField, this::isNotUsed, txt("msg.assistant.error.ProfileNameAlreadyInUse"));
        addSelectionValidation(providerComboBox, txt("msg.assistant.error.LlmProviderRequired"));
        addSelectionValidation(credentialComboBox, txt("msg.assistant.error.SelectOrCreateCredential"));
    }

    public void applyFormChanges() {
        profile.setName(getText(nameTextField));
        profile.setProviderId(getSelectedProviderId());
        profile.setCredentialId(getSelectedCredentialId());
        profile.setTemperaturePreset(getSelectedTemperature());
        profile.setTemperature(isCustomTemperature() ? temperatureSlider.getValue() / 100.0 : getSelectedTemperature().getValue());
        profile.setInstructions(getText(instructionsTextArea));
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

        instructionsTextArea.setText(profile.getInstructions());
    }

    private boolean isNotUsed(String name) {
        return !getRequest().getUsedNames().contains(name);
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
