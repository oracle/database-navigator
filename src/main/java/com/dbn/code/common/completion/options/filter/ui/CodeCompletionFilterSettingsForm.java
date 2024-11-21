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

package com.dbn.code.common.completion.options.filter.ui;

import com.dbn.code.common.completion.options.filter.CodeCompletionFilterSettings;
import com.dbn.common.color.Colors;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import java.awt.BorderLayout;

public class CodeCompletionFilterSettingsForm extends ConfigurationEditorForm<CodeCompletionFilterSettings> {
    private JPanel mainPanel;
    private CodeCompletionFilterTreeModel treeModel;

    public CodeCompletionFilterSettingsForm(CodeCompletionFilterSettings codeCompletionFilterSettings) {
        super(codeCompletionFilterSettings);
        treeModel = new CodeCompletionFilterTreeModel(codeCompletionFilterSettings);
        CodeCompletionFilterTree tree = new CodeCompletionFilterTree(treeModel);
        mainPanel = new JPanel(new BorderLayout());
        JBScrollPane scrollPane = new JBScrollPane(tree);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.setBackground(Colors.getListBackground());
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        treeModel.applyChanges();
    }

    @Override
    public void resetFormChanges() {
        treeModel.resetChanges();
    }
}
