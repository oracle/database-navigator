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

package com.dbn.assistant.credential.local.ui;


import com.dbn.assistant.credential.local.LocalCredential;
import com.dbn.assistant.credential.local.LocalCredentialBundle;
import com.dbn.assistant.credential.local.LocalCredentialSettings;
import com.dbn.common.action.BasicActionButton;
import com.dbn.common.icon.Icons;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.credentials.Secret;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.ToolbarDecorator;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import javax.swing.table.TableCellEditor;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dbn.common.ui.util.Decorators.createToolbarDecorator;
import static com.dbn.common.ui.util.Decorators.createToolbarDecoratorComponent;

public class LocalCredentialsSettingsForm extends ConfigurationEditorForm<LocalCredentialSettings> {
    private JPanel mainPanel;
    private JPanel credentialsTablePanel;

    private final LocalCredentialsEditorTable credentialsTable;

    public LocalCredentialsSettingsForm(LocalCredentialSettings settings) {
        super(settings);

        credentialsTable = new LocalCredentialsEditorTable(this, settings.getCredentials());
        credentialsTablePanel.add(initTableComponent());

        registerComponents(mainPanel);
    }

    private JPanel initTableComponent() {
        ToolbarDecorator decorator = createToolbarDecorator(credentialsTable);
        decorator.setAddAction(b -> credentialsTable.insertRow());
        decorator.setRemoveAction(b -> credentialsTable.removeRow());
        decorator.setMoveUpAction(b -> credentialsTable.moveRowUp());
        decorator.setMoveDownAction(b -> credentialsTable.moveRowDown());
        decorator.addExtraAction(new BasicActionButton("Revert Changes", null, Icons.ACTION_REVERT) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                TableCellEditor cellEditor = credentialsTable.getCellEditor();
                if (cellEditor != null) {
                    cellEditor.cancelCellEditing();
                }
                credentialsTable.setCredentials(getConfiguration().getCredentials());
            }

        });

        return createToolbarDecoratorComponent(decorator, credentialsTable);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        LocalCredentialSettings configuration = getConfiguration();

        // capture old secrets
        Map<Object, LocalCredential> oldCredentials = configuration
                .getCredentials()
                .getElements()
                .stream()
                .collect(Collectors.toMap(
                        o -> o.getSecretOwnerId(),
                        o -> o));

        LocalCredentialsTableModel model = credentialsTable.getModel();
        model.validate();

        List<LocalCredential> credentials = model.getElements();
        configuration.setCredentials(new LocalCredentialBundle(credentials));

        for (LocalCredential credential : credentials) {
            LocalCredential olsCredential = oldCredentials.remove(credential.getSecretOwnerId());
            Secret[] oldSecrets = olsCredential == null ? null : olsCredential.getSecrets();
            credential.updateSecrets(oldSecrets);
        }

        oldCredentials.values().forEach(c -> c.removeSecrets());
    }

    @Override
    public void resetFormChanges() {
        LocalCredentialSettings settings = getConfiguration();
        List<LocalCredential> credentials = settings.getCredentials().getElements();
        credentialsTable.getModel().setElements(credentials);
    }
}
