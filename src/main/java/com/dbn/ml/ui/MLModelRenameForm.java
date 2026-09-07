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

package com.dbn.ml.ui;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.util.Strings.isAlphanumericWithUnderscore;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.nls.NlsResources.txt;

/**
 * Form for renaming an ML model in the database.
 *
 * @author ayoub allali
 */
public class MLModelRenameForm extends DBNFormBase {
    private static final int MAX_MODEL_NAME_LENGTH = 128;

    private JPanel mainPanel;
    private JPanel hintPanel;
    private JLabel modelNameLabel;
    private JBTextField modelNameField;

    private final String currentModelName;

    MLModelRenameForm(MLModelRenameDialog parent, String currentModelName) {
        super(parent);
        this.currentModelName = currentModelName;

        initHintPanel();
        setText(modelNameField, currentModelName);
        modelNameField.selectAll();
    }

    private void initHintPanel() {
        TextContent hintContent = TextContent.plain(txt("msg.machineLearning.hint.RenameModel", currentModelName));
        DBNHintForm hintForm = new DBNHintForm(this, hintContent, null, true);
        hintPanel.add(hintForm.getComponent());
    }

    @Override
    protected void initValidation() {
        addTextValidation(modelNameField, n -> isNotEmptyOrSpaces(n), txt("msg.objects.error.ModelNameRequired"));
        addTextValidation(modelNameField, n -> n.length() <= MAX_MODEL_NAME_LENGTH, txt("msg.machineLearning.error.ModelNameTooLong", MAX_MODEL_NAME_LENGTH));
        // model names are interpolated into the scheduler PL/SQL when training - keep them plain identifiers
        addTextValidation(modelNameField, n -> isAlphanumericWithUnderscore(n), txt("msg.machineLearning.error.ModelNameInvalid"));
        addTextValidation(modelNameField, n -> startsWithLetter(n), txt("msg.machineLearning.error.ModelNameLeadingDigit"));
    }

    private static boolean startsWithLetter(String modelName) {
        return modelName.isEmpty() || Character.isLetter(modelName.charAt(0));
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return modelNameField;
    }

    /**
     * Returns the entered model name, normalized the way the database stores it.
     */
    public String getModelName() {
        return getText(modelNameField).trim().toUpperCase();
    }
}
