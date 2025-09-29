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

package com.dbn.assistant.profile.ui;


import com.dbn.assistant.credential.AssistantCredential;
import com.dbn.assistant.credential.AssistantCredentialSettings;
import com.dbn.assistant.credential.ui.AssistantCredentialsSettingsForm;
import com.dbn.assistant.profile.AssistantProfileBundle;
import com.dbn.assistant.profile.AssistantProfileSettings;
import com.dbn.assistant.profile.DeclaredAssistantProfile;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.routine.Consumer;
import com.dbn.common.ui.util.Mouse;
import com.dbn.common.util.Dialogs;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ToolbarDecorator;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dbn.common.ui.util.Decorators.createToolbarDecorator;
import static com.dbn.common.ui.util.Decorators.createToolbarDecoratorComponent;

public class AssistantProfilesSettingsForm extends ConfigurationEditorForm<AssistantProfileSettings> {
    private JPanel mainPanel;
    private JPanel profilesTablePanel;

    private final AssistantProfilesEditorTable profilesTable;

    public AssistantProfilesSettingsForm(AssistantProfileSettings settings) {
        super(settings);

        AssistantProfileBundle profiles = settings.getProfiles();
        profilesTable = new AssistantProfilesEditorTable(this, profiles, () -> getCredentials());
        profilesTablePanel.add(initTableComponent());

        registerComponents(mainPanel);
    }

    private JPanel initTableComponent() {
        ToolbarDecorator decorator = createToolbarDecorator(profilesTable);
        decorator.setAddAction(b -> openProfileEditor(true));
        decorator.setRemoveAction(b -> profilesTable.removeRow());
        decorator.setMoveUpAction(b -> profilesTable.moveRowUp());
        decorator.setMoveDownAction(b -> profilesTable.moveRowDown());
        decorator.setEditAction(b -> openProfileEditor(false));

        Mouse.onMouseDoubleClick(profilesTable, e -> openProfileEditor(false));
        return createToolbarDecoratorComponent(decorator, profilesTable);
    }

    private void openProfileEditor(boolean create) {
        DeclaredAssistantProfile selectedProfile = getSelectedProfile();
        if (selectedProfile == null && !create) return;

        DeclaredAssistantProfile profile = create ? null : selectedProfile;
        Consumer<DeclaredAssistantProfile> onSave = c -> {
            if (create) {
                AssistantProfilesTableModel model = profilesTable.getModel();
                model.addElement(c);
            }
            profilesTable.revalidate();
            profilesTable.repaint();
        };

        List<AssistantCredential> credentials = getCredentials();
        Set<String> profileNames = getProfileNames(profile == null ? null : profile.getName());
        Dialogs.show(() -> new AssistantProfileEditDialog(getProject(), profile, credentials, profileNames, onSave));
    }

    private List<AssistantCredential> getCredentials() {
        // support transient credentials if hosted inside the "Assistant" settings tab
        AssistantCredentialSettings credentialSettings = getConfiguration().ensureParent().getCredentialSettings();
        AssistantCredentialsSettingsForm credentialsSettingsForm = credentialSettings.getSettingsEditor();
        return credentialsSettingsForm == null ?
                credentialSettings.getCredentials().getElements() :
                credentialsSettingsForm.getCredentials();
    }

    private Set<String> getProfileNames(String excludeName) {
        return profilesTable
                .getModel()
                .getElements()
                .stream()
                .map(c -> c.getName())
                .filter(n -> !n.equals(excludeName))
                .collect(Collectors.toSet());
    }

    private DeclaredAssistantProfile getSelectedProfile() {
        AssistantProfilesTableModel model = profilesTable.getModel();
        if (model.isEmpty()) return null;

        int[] selectedIndices = profilesTable.getSelectionModel().getSelectedIndices();
        if (selectedIndices.length != 1) return null;

        List<DeclaredAssistantProfile> elements = model.getElements();
        return elements.get(selectedIndices[0]);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        AssistantProfilesTableModel model = profilesTable.getModel();
        model.validate();

        List<DeclaredAssistantProfile> profiles = model.getElements();

        AssistantProfileSettings configuration = getConfiguration();
        Project project = configuration.getProject();
        configuration.setProfiles(new AssistantProfileBundle(project, profiles));
    }

    @Override
    public void resetFormChanges() {
        AssistantProfileSettings settings = getConfiguration();
        List<DeclaredAssistantProfile> profiles = settings.getProfiles().getDeclaredProfiles();
        profilesTable.getModel().setElements(profiles);
    }
}
