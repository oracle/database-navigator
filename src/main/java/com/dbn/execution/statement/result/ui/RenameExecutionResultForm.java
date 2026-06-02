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
import com.dbn.execution.ExecutionResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

import static com.dbn.common.text.TextContent.plain;
import static com.dbn.common.ui.util.TextFields.getText;

public class RenameExecutionResultForm extends DBNFormBase {
    private JPanel headerPanel;
    private JPanel mainPanel;
    private JPanel hintPanel;
    private JTextField nameTextField;
    private JCheckBox stickyCheckBox;

    RenameExecutionResultForm(RenameExecutionResultDialog parent, @NotNull ExecutionResult executionResult) {
        super(parent);

        DBNHeaderForm headerForm = new DBNHeaderForm(this,
                "Execution result - " + executionResult.getName(),
                executionResult.getIcon(),
                executionResult.getConnection().getEnvironmentType().getColor());
        headerPanel.add(headerForm.getComponent());

        String resultName = executionResult.getName();
        nameTextField.setText(resultName);

        if (executionResult.supportsStickyNames()) {
            TextContent hint = plain("Use \"Sticky\" option to retain the name after the result is closed.");
            DBNHintForm hintForm = new DBNHintForm(this, hint, null, false);
            hintPanel.add(hintForm.getComponent());

            ExecutionManager executionManager = ExecutionManager.getInstance(ensureProject());
            stickyCheckBox.setSelected(executionManager.isRetainStickyNames());
        } else {
            hintPanel.setVisible(false);
            stickyCheckBox.setVisible(false);
        }
    }

    protected void initValidation() {
        addTextValidation(nameTextField, n -> !Strings.isEmpty(n), txt("msg.execution.error.ResultNameRequired"));
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return nameTextField;
    }

    public String getResultName() {
        return getText(nameTextField);
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
