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

package com.dbn.generator.code.shared.ui;

import com.dbn.common.outcome.DialogCloseOutcomeHandler;
import com.dbn.common.outcome.OutcomeType;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.generator.code.CodeGeneratorContext;
import com.dbn.generator.code.CodeGeneratorManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

@Getter
public class CodeGeneratorInputDialog extends DBNDialog<CodeGeneratorInputForm> {
    private final CodeGeneratorContext context;

    public CodeGeneratorInputDialog(CodeGeneratorContext context) {
        super(context.getProject(), "Generate Code (" + context.getGeneratorName() + ")", false);
        this.context = context;

        // add handler to close the dialog on success
        this.context.addOutcomeHandler(OutcomeType.SUCCESS, DialogCloseOutcomeHandler.create(this));
        init();
    }

    @NotNull
    @Override
    protected CodeGeneratorInputForm createForm() {
        CodeGeneratorManager manager = getCodeGenerationManager();
        return manager.createInputForm(this, context);
    }

    private void generateCode() {
        // apply the form field values to the input
        CodeGeneratorInputForm inputForm = getForm();
        inputForm.applyUserInput();

        CodeGeneratorManager manager = getCodeGenerationManager();
        manager.generateCode(context);
    }

    @NotNull
    private CodeGeneratorManager getCodeGenerationManager() {
        return CodeGeneratorManager.getInstance(getProject());
    }


    @NotNull
    @Override
    protected final Action[] createActions() {
        return new Action[]{
                getOKAction(),
                getCancelAction()};
    }

    @Override
    protected void doOKAction() {
        generateCode();
    }
}
