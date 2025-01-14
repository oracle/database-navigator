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

package com.dbn.connection.transaction.options.ui;

import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.connection.transaction.TransactionOption;
import com.dbn.connection.transaction.options.TransactionManagerSettings;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static com.dbn.common.ui.util.ClientProperty.COMPONENT_GROUP_QUALIFIER;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;

public class TransactionManagerSettingsForm extends ConfigurationEditorForm<TransactionManagerSettings> {
    private JPanel mainPanel;
    private JComboBox<TransactionOption> uncommittedChangesOnProjectCloseComboBox;
    private JComboBox<TransactionOption> uncommittedChangesOnSwitchComboBox;
    private JComboBox<TransactionOption> uncommittedChangesOnDisconnectComboBox;
    private JComboBox<TransactionOption> multipleChangesOnCommitComboBox;
    private JComboBox<TransactionOption> multipleChangesOnRollbackComboBox;
    private JLabel uncommitedChangesLabel;
    private JLabel transactionHandlingLabel;

    public TransactionManagerSettingsForm(TransactionManagerSettings settings) {
        super(settings);

        initComboBox(uncommittedChangesOnProjectCloseComboBox,
                TransactionOption.ASK,
                TransactionOption.COMMIT,
                TransactionOption.ROLLBACK,
                TransactionOption.REVIEW_CHANGES);


        initComboBox(uncommittedChangesOnSwitchComboBox,
                TransactionOption.ASK,
                TransactionOption.COMMIT,
                TransactionOption.ROLLBACK,
                TransactionOption.REVIEW_CHANGES);

        initComboBox(uncommittedChangesOnDisconnectComboBox,
                TransactionOption.ASK,
                TransactionOption.COMMIT,
                TransactionOption.ROLLBACK,
                TransactionOption.REVIEW_CHANGES);

        initComboBox(multipleChangesOnCommitComboBox,
                TransactionOption.ASK,
                TransactionOption.COMMIT,
                TransactionOption.REVIEW_CHANGES);

        initComboBox(multipleChangesOnRollbackComboBox,
                TransactionOption.ASK,
                TransactionOption.ROLLBACK,
                TransactionOption.REVIEW_CHANGES);

        resetFormChanges();
        registerComponent(mainPanel);
    }

    @Override
    protected void initAccessibility() {
        // mark group header labels as component group qualifiers
        COMPONENT_GROUP_QUALIFIER.set(uncommitedChangesLabel, true);
        COMPONENT_GROUP_QUALIFIER.set(transactionHandlingLabel, true);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        TransactionManagerSettings settings = getConfiguration();
        settings.getCloseProject().set(           getSelection(uncommittedChangesOnProjectCloseComboBox));
        settings.getToggleAutoCommit().set(       getSelection(uncommittedChangesOnSwitchComboBox));
        settings.getDisconnect().set(             getSelection(uncommittedChangesOnDisconnectComboBox));
        settings.getCommitMultipleChanges().set(  getSelection(multipleChangesOnCommitComboBox));
        settings.getRollbackMultipleChanges().set(getSelection(multipleChangesOnRollbackComboBox));
    }

    @Override
    public void resetFormChanges() {
        TransactionManagerSettings settings = getConfiguration();
        setSelection(uncommittedChangesOnProjectCloseComboBox, settings.getCloseProject().get());
        setSelection(uncommittedChangesOnSwitchComboBox,       settings.getToggleAutoCommit().get());
        setSelection(uncommittedChangesOnDisconnectComboBox,   settings.getDisconnect().get());
        setSelection(multipleChangesOnCommitComboBox,          settings.getCommitMultipleChanges().get());
        setSelection(multipleChangesOnRollbackComboBox,        settings.getRollbackMultipleChanges().get());

    }
}
