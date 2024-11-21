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

package com.dbn.debugger.common.config.ui;

import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.debugger.ExecutionConfigManager;
import com.dbn.debugger.common.config.DBRunConfigCategory;
import com.dbn.debugger.common.config.DBStatementRunConfig;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;

public class DBStatementRunConfigForm extends DBProgramRunConfigForm<DBStatementRunConfig> {
    private JPanel headerPanel;
    private JPanel mainPanel;
    private JPanel hintPanel;

    public DBStatementRunConfigForm(DBStatementRunConfig configuration) {
        super(configuration.getProject(), configuration.getDebuggerType());
        if (configuration.getCategory() != DBRunConfigCategory.CUSTOM) {
            headerPanel.setVisible(false);
            DBNHintForm hintForm = new DBNHintForm(this, ExecutionConfigManager.GENERIC_STATEMENT_RUNNER_HINT, null, true);
            hintPanel.setVisible(true);
            hintPanel.add(hintForm.getComponent());
        } else {
            hintPanel.setVisible(false);
        }

    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void writeConfiguration(DBStatementRunConfig configuration) {
    }

    @Override
    public void readConfiguration(DBStatementRunConfig configuration) {
    }
}
