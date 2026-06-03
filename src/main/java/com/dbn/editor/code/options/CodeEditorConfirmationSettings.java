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

package com.dbn.editor.code.options;

import com.dbn.common.option.InteractiveConfirmationBroker;
import com.dbn.common.option.InteractiveOptionBroker;
import com.dbn.common.options.BasicConfiguration;
import com.dbn.editor.code.options.ui.CodeEditorConfirmationSettingsForm;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

@Getter
@EqualsAndHashCode(callSuper = false)
public class CodeEditorConfirmationSettings extends BasicConfiguration<CodeEditorSettings, CodeEditorConfirmationSettingsForm> {
    public static final String REMEMBER_OPTION_HINT = ""; //"\n\n(you can remember your option and change it at any time in Settings > Operations > Session Manager)";

    private final InteractiveConfirmationBroker saveChanges =
            new InteractiveConfirmationBroker(
                    "save-changes",
                    txt("msg.codeEditor.title.SaveChanges"),
                    txt("msg.codeEditor.question.SaveChanges"), false);

    private final InteractiveConfirmationBroker revertChanges =
            new InteractiveConfirmationBroker(
                    "revert-changes",
                    txt("msg.codeEditor.title.RevertChanges"),
                    txt("msg.codeEditor.question.RevertChanges"), true);

    private final InteractiveOptionBroker<CodeEditorChangesOption> exitOnChanges =
            new InteractiveOptionBroker<>(
                    "exit-on-changes",
                    txt("msg.codeEditor.title.UnsavedChanges"),
                    txt("msg.codeEditor.question.CloseEditorWithUnsavedChanges"),
                    CodeEditorChangesOption.ASK,
                    CodeEditorChangesOption.SAVE,
                    CodeEditorChangesOption.DISCARD,
                    CodeEditorChangesOption.SHOW,
                    CodeEditorChangesOption.CANCEL);


    private final InteractiveOptionBroker<CodeEditorChangesOption> temporaryConsole =
            new InteractiveOptionBroker<>(
                    "close-temporary-console",
                    txt("msg.codeEditor.title.TemporaryConsole"),
                    txt("msg.codeEditor.question.CloseTemporaryConsole"),
                    CodeEditorChangesOption.ASK,
                    CodeEditorChangesOption.SAVE,
                    CodeEditorChangesOption.DISCARD,
                    CodeEditorChangesOption.CANCEL);


    public CodeEditorConfirmationSettings(CodeEditorSettings parent) {
        super(parent);
    }

    @Override
    public String getDisplayName() {
        return txt("cfg.codeEditor.title.ConfirmationSettings");
    }

    /****************************************************
     *                   Configuration                  *
     ****************************************************/
    @Override
    @NotNull
    public CodeEditorConfirmationSettingsForm createConfigurationEditor() {
        return new CodeEditorConfirmationSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "confirmations";
    }

    @Override
    public void readConfiguration(Element element) {
        saveChanges.readConfiguration(element);
        revertChanges.readConfiguration(element);
        exitOnChanges.readConfiguration(element);
        temporaryConsole.readConfiguration(element);
    }

    @Override
    public void writeConfiguration(Element element) {
        saveChanges.writeConfiguration(element);
        revertChanges.writeConfiguration(element);
        exitOnChanges.writeConfiguration(element);
        temporaryConsole.writeConfiguration(element);
    }
}
