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

package com.dbn.execution.statement.result.ui;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.util.Strings;
import com.dbn.execution.ExecutionManager;
import com.dbn.execution.statement.result.StatementExecutionResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;

import static com.dbn.common.text.TextContent.plain;

public class RenameExecutionResultForm extends DBNFormBase {
    private JPanel headerPanel;
    private JPanel mainPanel;
    private JPanel hintPanel;
    private JTextField resultNameTextField;
    private JCheckBox stickyCheckBox;

    RenameExecutionResultForm(RenameExecutionResultDialog parent, @NotNull StatementExecutionResult executionResult) {
        super(parent);

        DBNHeaderForm headerForm = new DBNHeaderForm(this,
                "Execution result - " + executionResult.getName(),
                executionResult.getIcon(),
                executionResult.getConnection().getEnvironmentType().getColor());
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

        TextContent hint = plain("Use \"Sticky\" option to retain the name after the result is closed.");
        DBNHintForm hintForm = new DBNHintForm(this, hint, null, false);
        hintPanel.add(hintForm.getComponent(), BorderLayout.CENTER);

        String resultName = executionResult.getName();
        resultNameTextField.setText(resultName);

        ExecutionManager executionManager = ExecutionManager.getInstance(ensureProject());
        stickyCheckBox.setSelected(executionManager.isRetainStickyNames());
    }

    protected void initValidation() {
        addTextValidation(resultNameTextField, n -> !Strings.isEmpty(n), "Please specify a result name");
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return resultNameTextField;
    }

    public String getResultName() {
        return resultNameTextField.getText();
    }

    public boolean isStickyResultName() {
        return stickyCheckBox.isSelected();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
