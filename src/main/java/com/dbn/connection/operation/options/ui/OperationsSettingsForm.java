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

package com.dbn.connection.operation.options.ui;

import com.dbn.common.options.ui.CompositeConfigurationEditorForm;
import com.dbn.connection.operation.options.OperationSettings;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import java.awt.BorderLayout;

public class OperationsSettingsForm extends CompositeConfigurationEditorForm<OperationSettings> {
    private JPanel mainPanel;
    private JPanel transactionSettingsPanel;
    private JPanel sessionBrowserPanel;
    private JPanel compilerPanel;

    public OperationsSettingsForm(OperationSettings settings) {
        super(settings);
        transactionSettingsPanel.add(settings.getTransactionManagerSettings().createComponent(), BorderLayout.CENTER);
        sessionBrowserPanel.add(settings.getSessionBrowserSettings().createComponent(), BorderLayout.CENTER);
        compilerPanel.add(settings.getCompilerSettings().createComponent(), BorderLayout.CENTER);
        resetFormChanges();
    }


    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
