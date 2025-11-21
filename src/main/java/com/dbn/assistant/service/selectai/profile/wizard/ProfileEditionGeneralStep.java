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

package com.dbn.assistant.service.selectai.profile.wizard;

import com.dbn.assistant.service.selectai.credential.ui.CredentialEditDialog;
import com.dbn.assistant.service.selectai.profile.wizard.validation.OciCompartmentIdVerifier;
import com.dbn.assistant.service.selectai.profile.wizard.validation.ProfileCredentialVerifier;
import com.dbn.assistant.service.selectai.profile.wizard.validation.ProfileNameVerifier;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Background;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Lists;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.object.DBCredential;
import com.dbn.object.DBSchema;
import com.dbn.object.event.ObjectChangeListener;
import com.dbn.object.type.DBCredentialType;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import org.jetbrains.annotations.Nullable;

import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.UserInterface.updateTitledBorders;
import static com.dbn.common.ui.util.UserInterface.whenFirstShown;
import static com.dbn.common.util.Commons.nvln;
import static com.dbn.common.util.Lists.convert;
import static com.dbn.common.util.Lists.filter;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.type.DBCredentialType.getSelectAITypes;

/**
 * Profile edition general step for edition wizard.
 *
 * @see ProfileEditionWizard
 */
public class ProfileEditionGeneralStep extends WizardStep<ProfileEditionWizardModel> implements Disposable {
    private JPanel mainPanel;
    private JTextField nameTextField;
    private JComboBox<String> credentialComboBox;
    private JTextField descriptionTextField;
    private JButton addCredentialButton;
    private JTextField regionTextField;
    private JLabel regionLabel;
    private JLabel ociCompartmentIdLabel;
    private JTextField ociCompartmentIdTextField;
    private JLabel ociEndpointIdLabel;
    private JTextField ociEndpointIdTextField;
    private JLabel ociRuntimeTypeLabel;
    private JTextField ociRuntimeTypeTextField;
    private JLabel ociApiFormatLabel;
    private JTextField ociApiFormatTextField;
    private JPanel ociAttributesPanel;

    private final Map<String, DBCredentialType> credentialTypes = new HashMap<>();
    private final ConnectionRef connection;
    private final ProfileData profile;
    private final Set<String> existingProfileNames;

    private final boolean isUpdate;

    public ProfileEditionGeneralStep(ConnectionHandler connection, ProfileData profile, Set<String> existingProfileNames, boolean isUpdate) {
        super(txt("cfg.assistant.title.GeneralSettings"),
                txt("cfg.assistant.text.GeneralSettings"));
        this.connection = ConnectionRef.of(connection);
        this.profile = profile;
        this.existingProfileNames = existingProfileNames;
        this.isUpdate = isUpdate;

        initCredentialAddButton();
        initializeUI();
        addValidationListener();

        updateTitledBorders(mainPanel);

        whenFirstShown(mainPanel, () -> populateCredentials());
    }

    private void initCredentialAddButton() {
        addCredentialButton.setIcon(Icons.ACTION_ADD);
        addCredentialButton.setText(null);

        ConnectionHandler connection = getConnection();
        addCredentialButton.addActionListener(e -> Dialogs.show(() -> new CredentialEditDialog(
                connection, null,
                getSelectAITypes(),
                Set.of())));

        Project project = connection.getProject();
        ProjectEvents.subscribe(project, this, ObjectChangeListener.TOPIC, e -> {
            if (!e.matches(connection)) return;
            if (!e.matches(DBObjectType.CREDENTIAL)) return;

            populateCredentials();
        });
    }

    ConnectionHandler getConnection() {
        return ConnectionRef.ensure(connection);
    }

    private void initializeUI() {
        if (isUpdate) {
            nameTextField.setText(profile.getName());
            descriptionTextField.setText(profile.getDescription());
            regionTextField.setText(profile.getRegion());
            ociCompartmentIdTextField.setText(profile.getOciCompartmentId());
            ociEndpointIdTextField.setText(profile.getOciEndpointId());
            ociRuntimeTypeTextField.setText(profile.getOciRuntimeType());
            ociApiFormatTextField.setText(profile.getOciApiFormat());
            nameTextField.setEnabled(false);
            credentialComboBox.setEnabled(true);
            descriptionTextField.setEnabled(false);
        }
    }

    private void addValidationListener() {
        nameTextField.setInputVerifier(new ProfileNameVerifier(existingProfileNames, isUpdate));
        credentialComboBox.setInputVerifier(new ProfileCredentialVerifier());
        ociCompartmentIdTextField.setInputVerifier(new OciCompartmentIdVerifier()); // Add this line

        ((AbstractDocument) nameTextField.getDocument()).setDocumentFilter(new UppercaseDocumentFilter());

        installValidator(nameTextField);
        installValidator(ociCompartmentIdTextField);

        credentialComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                InputVerifier verifier = credentialComboBox.getInputVerifier();
                if (verifier != null) {
                    verifier.verify(credentialComboBox);
                }
            }
            ociAttributesPanel.setVisible(isOciCredential());
        });
    }

    private static void installValidator(JTextField textField) {
        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                validateInput(textField);
            }

            public void removeUpdate(DocumentEvent e) {
                validateInput(textField);
            }

            public void insertUpdate(DocumentEvent e) {
                validateInput(textField);
            }
        });
    }

    private static boolean validateInput(JComponent component) {
        return component.getInputVerifier().verify(component);
    }

    private boolean isOciCredential() {
        return getSelectedCredentialType() == DBCredentialType.OCI;
    }

    private DBCredentialType getSelectedCredentialType() {
        String selectedName = (String) credentialComboBox.getSelectedItem();
        return credentialTypes.get(selectedName);
    }

    private void populateCredentials() {
        Background.run(() -> {
            String currentCredential = profile.getCredentialName();
            ConnectionHandler connection = getConnection();
            DBSchema schema = connection.getObjectBundle().getUserSchema();
            if (schema == null) return;

            List<DBCredential> credentials = schema.getCredentials();
            credentials = filter(credentials, c -> getSelectAITypes().contains(c.getType()));
            credentialTypes.clear();
            credentials.forEach(c -> credentialTypes.put(c.getName(), c.getType()));

            List<String> credentialNames = convert(credentials, c -> c.getName());
            if (currentCredential != null && !credentialNames.contains(currentCredential))
                credentialNames.add(currentCredential);

            credentialComboBox.removeAllItems();
            credentialNames.forEach(c -> credentialComboBox.addItem(c));
            String selectedCredential = nvln(currentCredential, Lists.firstElement(credentialNames));
            credentialComboBox.setSelectedItem(selectedCredential);
        });
    }

    @Override
    public JComponent prepare(WizardNavigationState wizardNavigationState) {
        return mainPanel;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return nameTextField;
    }

    @Override
    public WizardStep<ProfileEditionWizardModel> onNext(ProfileEditionWizardModel model) {
        boolean nameValid = isUpdate || validateInput(nameTextField);
        boolean credentialValid = validateInput(credentialComboBox);
        boolean ociCompartmentIdValid = validateInput(ociCompartmentIdTextField);

        profile.setName(getText(nameTextField));
        profile.setCredentialName((String) credentialComboBox.getSelectedItem());

        boolean ociCredential = isOciCredential();

        String region = getText(regionTextField);
        if (ociCredential && !region.isEmpty()) {
            profile.setRegion(region);
        } else {
            profile.setRegion(null);
        }

        String ociCompartmentId = getText(ociCompartmentIdTextField);
        if (ociCredential && !ociCompartmentId.isEmpty()) {
            profile.setOciCompartmentId(ociCompartmentId);
        } else {
            profile.setOciCompartmentId(null);
        }

        String ociEndpointId = getText(ociEndpointIdTextField);
        if (ociCredential && !ociEndpointId.isEmpty()) {
            profile.setOciEndpointId(ociEndpointId);
        } else {
            profile.setOciEndpointId(null);
        }

        String ociRuntimeType = getText(ociRuntimeTypeTextField);
        if (ociCredential && !ociRuntimeType.isEmpty()) {
            profile.setOciRuntimeType(ociRuntimeType);
        } else {
            profile.setOciRuntimeType(null);
        }

        String ociApiFormat = getText(ociApiFormatTextField);
        if (ociCredential && !ociApiFormat.isEmpty()) {
            profile.setOciApiFormat(ociApiFormat);
        } else {
            profile.setOciApiFormat(null);
        }

        // Handle description logic...
        String description = getText(descriptionTextField);
        if (description.isEmpty()) {
            if (profile.getDescription() != null && !profile.getDescription().isEmpty()) {
                profile.setDescription(description);
            }
        } else {
            profile.setDescription(description);
        }

        return nameValid && credentialValid && ociCompartmentIdValid ? super.onNext(model) : this;
    }

    @Override
    public void dispose() {
        // TODO dispose UI resources
    }

    private static class UppercaseDocumentFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, javax.swing.text.AttributeSet attr) throws javax.swing.text.BadLocationException {
            super.insertString(fb, offset, string.toUpperCase(), attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, javax.swing.text.AttributeSet attrs) throws javax.swing.text.BadLocationException {
            super.replace(fb, offset, length, text.toUpperCase(), attrs);
        }
    }
}
