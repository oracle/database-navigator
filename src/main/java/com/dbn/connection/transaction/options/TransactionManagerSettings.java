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

package com.dbn.connection.transaction.options;

import com.dbn.common.option.InteractiveOptionBroker;
import com.dbn.common.options.BasicConfiguration;
import com.dbn.connection.operation.options.OperationSettings;
import com.dbn.connection.transaction.TransactionOption;
import com.dbn.connection.transaction.options.ui.TransactionManagerSettingsForm;
import com.dbn.help.HelpTopic;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.help.HelpTopic.TRANSACTION_HANDLING;
import static com.dbn.nls.NlsResources.txt;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class TransactionManagerSettings extends BasicConfiguration<OperationSettings, TransactionManagerSettingsForm> {
    public static final String REMEMBER_OPTION_HINT = ""/*"\n\n(you can remember your option and change it at any time in Settings > Operations > Transaction Manager)"*/;

    private final InteractiveOptionBroker<TransactionOption> closeProject =
            new InteractiveOptionBroker<>(
                    "on-project-close",
                    txt("msg.transactions.title.OpenTransactions"),
                    txt("msg.transactions.question.CloseProjectWithOpenTransactions"),
                    TransactionOption.ASK,
                    TransactionOption.COMMIT,
                    TransactionOption.ROLLBACK,
                    TransactionOption.REVIEW_CHANGES,
                    TransactionOption.CANCEL);

    private final InteractiveOptionBroker<TransactionOption> toggleAutoCommit =
            new InteractiveOptionBroker<>(
                    "on-autocommit-toggle",
                    txt("msg.transactions.title.OpenTransactions"),
                    txt("msg.transactions.question.ToggleAutoCommitWithOpenTransactions"),
                    TransactionOption.ASK,
                    TransactionOption.COMMIT,
                    TransactionOption.ROLLBACK,
                    TransactionOption.REVIEW_CHANGES,
                    TransactionOption.CANCEL);

    private final InteractiveOptionBroker<TransactionOption> disconnect =
            new InteractiveOptionBroker<>(
                    "on-disconnect",
                    txt("msg.transactions.title.OpenTransactions"),
                    txt("msg.transactions.question.DisconnectWithOpenTransactions"),
                    TransactionOption.ASK,
                    TransactionOption.COMMIT,
                    TransactionOption.ROLLBACK,
                    TransactionOption.REVIEW_CHANGES,
                    TransactionOption.CANCEL);

    private final InteractiveOptionBroker<TransactionOption> commitMultipleChanges =
            new InteractiveOptionBroker<>(
                    "on-commit",
                    txt("msg.transactions.title.CommitMultipleChanges"),
                    txt("msg.transactions.question.CommitMultipleChanges"),
                    TransactionOption.ASK,
                    TransactionOption.COMMIT,
                    TransactionOption.REVIEW_CHANGES,
                    TransactionOption.CANCEL);

    private final InteractiveOptionBroker<TransactionOption> rollbackMultipleChanges =
            new InteractiveOptionBroker<>(
                    "on-rollback",
                    txt("msg.transactions.title.RollbackMultipleChanges"),
                    txt("msg.transactions.question.RollbackMultipleChanges"),
                    TransactionOption.ASK,
                    TransactionOption.ROLLBACK,
                    TransactionOption.REVIEW_CHANGES,
                    TransactionOption.CANCEL);

    public TransactionManagerSettings(OperationSettings parent) {
        super(parent);
    }

    @Override
    public String getDisplayName() {
        return txt("cfg.transactions.title.TransactionManager");
    }

    @Override
    public HelpTopic getConfigHelpTopic() {
        return TRANSACTION_HANDLING;
    }

    /****************************************************
     *                   Configuration                  *
     ****************************************************/
    @Override
    @NotNull
    public TransactionManagerSettingsForm createConfigurationEditor() {
        return new TransactionManagerSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "transactions";
    }

    @Override
    public void readConfiguration(Element element) {
        Element uncommittedChangesElement = element.getChild("uncommitted-changes");
        if (uncommittedChangesElement != null) {
            closeProject.readConfiguration(uncommittedChangesElement);
            disconnect.readConfiguration(uncommittedChangesElement);
            toggleAutoCommit.readConfiguration(uncommittedChangesElement);
        }
        Element multipleUncommittedChangesElement = element.getChild("multiple-uncommitted-changes");
        if (multipleUncommittedChangesElement != null) {
            commitMultipleChanges.readConfiguration(uncommittedChangesElement);
            rollbackMultipleChanges.readConfiguration(uncommittedChangesElement);
        }
    }

    @Override
    public void writeConfiguration(Element element) {
        Element uncommittedChangesElement = newElement(element, "uncommitted-changes");
        closeProject.writeConfiguration(uncommittedChangesElement);
        disconnect.writeConfiguration(uncommittedChangesElement);
        toggleAutoCommit.writeConfiguration(uncommittedChangesElement);

        Element multipleUncommittedChangesElement = newElement(element, "multiple-uncommitted-changes");
        commitMultipleChanges.writeConfiguration(multipleUncommittedChangesElement);
        rollbackMultipleChanges.writeConfiguration(multipleUncommittedChangesElement);

    }
}
