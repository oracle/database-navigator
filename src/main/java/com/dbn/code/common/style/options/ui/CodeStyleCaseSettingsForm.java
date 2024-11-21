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

package com.dbn.code.common.style.options.ui;

import com.dbn.code.common.style.options.CodeStyleCase;
import com.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.ui.util.Keyboard;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;

public class CodeStyleCaseSettingsForm extends ConfigurationEditorForm<CodeStyleCaseSettings> {
    private JPanel mainPanel;
    private JComboBox<CodeStyleCase> keywordCaseComboBox;
    private JComboBox<CodeStyleCase> functionCaseComboBox;
    private JComboBox<CodeStyleCase> parameterCaseComboBox;
    private JComboBox<CodeStyleCase> datatypeCaseComboBox;
    private JComboBox<CodeStyleCase> objectCaseComboBox;
    private JCheckBox enableCheckBox;

    public static final CodeStyleCase[] OBJECT_STYLE_CASES = new CodeStyleCase[]{
            CodeStyleCase.PRESERVE,
            CodeStyleCase.UPPER,
            CodeStyleCase.LOWER,
            CodeStyleCase.CAPITALIZED};

    public static final CodeStyleCase[] KEYWORD_STYLE_CASES = new CodeStyleCase[]{
            CodeStyleCase.UPPER,
            CodeStyleCase.LOWER,
            CodeStyleCase.CAPITALIZED};

    public CodeStyleCaseSettingsForm(CodeStyleCaseSettings settings) {
        super(settings);
        initComboBox(keywordCaseComboBox, KEYWORD_STYLE_CASES);
        initComboBox(functionCaseComboBox, KEYWORD_STYLE_CASES);
        initComboBox(parameterCaseComboBox, KEYWORD_STYLE_CASES);
        initComboBox(datatypeCaseComboBox, KEYWORD_STYLE_CASES);
        initComboBox(objectCaseComboBox, OBJECT_STYLE_CASES);
        resetFormChanges();
        enableDisableOptions();

        Shortcut[] codeFormat = Keyboard.getShortcuts(IdeActions.ACTION_EDITOR_REFORMAT);

        enableCheckBox.setText("Allow case auto-format (" + KeymapUtil.getShortcutsText(codeFormat) + ')');


        registerComponent(mainPanel);
        enableCheckBox.addActionListener(e -> enableDisableOptions());



        //Shortcut[] basicShortcuts = KeyUtil.getShortcuts("ReformatCode");
        //enableCheckBox.setText("Use on reformat code (" + KeymapUtil.getShortcutsText(basicShortcuts) + ")");
    }

    private void enableDisableOptions() {
        boolean enabled = enableCheckBox.isSelected();
/*
        keywordCaseComboBox.setEnabled(enabled);
        functionCaseComboBox.setEnabled(enabled);
        parameterCaseComboBox.setEnabled(enabled);
        datatypeCaseComboBox.setEnabled(enabled);
        objectCaseComboBox.setEnabled(enabled);
*/
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        CodeStyleCaseSettings settings = getConfiguration();
        settings.getKeywordCaseOption().setStyleCase(getSelection(keywordCaseComboBox));
        settings.getFunctionCaseOption().setStyleCase(getSelection(functionCaseComboBox));
        settings.getParameterCaseOption().setStyleCase(getSelection(parameterCaseComboBox));
        settings.getDatatypeCaseOption().setStyleCase(getSelection(datatypeCaseComboBox));
        settings.getObjectCaseOption().setStyleCase(getSelection(objectCaseComboBox));
        settings.setEnabled(enableCheckBox.isSelected());
    }

    @Override
    public void resetFormChanges() {
        CodeStyleCaseSettings settings = getConfiguration();
        setSelection(keywordCaseComboBox, settings.getKeywordCaseOption().getStyleCase());
        setSelection(functionCaseComboBox, settings.getFunctionCaseOption().getStyleCase());
        setSelection(parameterCaseComboBox, settings.getParameterCaseOption().getStyleCase());
        setSelection(datatypeCaseComboBox, settings.getDatatypeCaseOption().getStyleCase());
        setSelection(objectCaseComboBox, settings.getObjectCaseOption().getStyleCase());
        enableCheckBox.setSelected(settings.isEnabled());
    }
}
