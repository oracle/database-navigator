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

package com.dbn.assistant.credential.ui;


import com.dbn.assistant.credential.AssistantCredential;
import com.dbn.assistant.credential.AssistantCredentialBundle;
import com.dbn.assistant.credential.AssistantCredentialSettings;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.ui.util.Mouse;
import com.dbn.common.util.Dialogs;
import com.dbn.credentials.Secret;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.ToolbarDecorator;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dbn.common.ui.util.Decorators.createToolbarDecorator;
import static com.dbn.common.ui.util.Decorators.createToolbarDecoratorComponent;

public class AssistantCredentialsSettingsForm extends ConfigurationEditorForm<AssistantCredentialSettings> {
    private JPanel mainPanel;
    private JPanel credentialsTablePanel;

    private final AssistantCredentialsEditorTable credentialsTable;

    public AssistantCredentialsSettingsForm(AssistantCredentialSettings settings) {
        super(settings);

        credentialsTable = new AssistantCredentialsEditorTable(this, settings.getCredentials());
        credentialsTablePanel.add(initTableComponent());

        registerComponents(mainPanel);
    }

    private JPanel initTableComponent() {
        ToolbarDecorator decorator = createToolbarDecorator(credentialsTable);
        decorator.setAddAction(b -> openCredentialEditor(true));
        decorator.setRemoveAction(b -> credentialsTable.removeRow());
        decorator.setMoveUpAction(b -> credentialsTable.moveRowUp());
        decorator.setMoveDownAction(b -> credentialsTable.moveRowDown());
        decorator.setEditAction(b -> openCredentialEditor(false));

        Mouse.onMouseDoubleClick(credentialsTable, e -> openCredentialEditor(false));
        return createToolbarDecoratorComponent(decorator, credentialsTable);
    }

    public void openCredentialEditor(boolean create) {
        AssistantCredential selectedCredential = getSelectedCredential();
        if (selectedCredential == null && !create) return;

        AssistantCredential credential = create ? null : selectedCredential;
        AssistantCredentialEditRequest request = AssistantCredentialEditRequest
                .builder()
                .credential(credential)
                .credentials(getConfiguration().getCredentials())
                .saveConsumer(c -> saveCredential(c, create))
                .build();


        Dialogs.show(() -> new AssistantCredentialEditDialog(getProject(), request));
    }

    private void saveCredential(AssistantCredential credential, boolean create) {
        if (create) {
            AssistantCredentialsTableModel model = credentialsTable.getModel();
            model.addElement(credential);
        }
        mackConfigModified();
        credentialsTable.revalidate();
        credentialsTable.repaint();
    }

    private AssistantCredential getSelectedCredential() {
        int[] selectedIndices = credentialsTable.getSelectionModel().getSelectedIndices();
        if (selectedIndices.length != 1) return null;

        return credentialsTable.getModel().getElements().get(selectedIndices[0]);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        AssistantCredentialSettings configuration = getConfiguration();

        // capture old secrets
        AssistantCredentialsTableModel model = credentialsTable.getModel();
        Map<Object, AssistantCredential> oldCredentials = model
                .getOriginalElements()
                .stream()
                .collect(Collectors.toMap(
                        o -> o.getSecretOwnerId(),
                        o -> o));


        model.validate();
        model.applyChanges();

        List<AssistantCredential> credentials = model.getElements();
        configuration.setCredentials(new AssistantCredentialBundle(getProject(), credentials));

        for (AssistantCredential credential : credentials) {
            AssistantCredential olsCredential = oldCredentials.remove(credential.getSecretOwnerId());
            Secret[] oldSecrets = olsCredential == null ? null : olsCredential.getSecrets();
            credential.updateSecrets(oldSecrets);
        }

        oldCredentials.values().forEach(c -> c.removeSecrets());
    }

    @Override
    public void resetFormChanges() {
        credentialsTable.getModel().resetChanges();
    }
}
