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

package com.dbn.connection.config.ui.easyconn;

import com.dbn.common.outcome.DialogCloseOutcomeHandler;
import com.dbn.common.outcome.OutcomeType;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.generator.code.CodeGeneratorContext;
import com.dbn.generator.code.CodeGeneratorManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * A dialog that collects Easy Connect URL-specific parameters.  These are distinct from the regular driver
 * "properties" that are set elsewhere.  The dialog is meant be called with any existing set parameters
 * parsed from a syntactically valid Easy Connect URL and will save values back to the same map.
 */
@Getter
public class EasyConnectUrlParameterInputDialog extends DBNDialog<EasyConnectUrlParameterInputForm> {
    private final EasyConnectUrlParameterContext context;

    public EasyConnectUrlParameterInputDialog(EasyConnectUrlParameterContext context) {
        super(null, "Set Easy Connect Parameters", false);
        this.context = context;

        // add handler to close the dialog on success
        this.context.addOutcomeHandler(OutcomeType.SUCCESS, DialogCloseOutcomeHandler.create(this));
        init();
    }

    @Override
    protected void init() {
        super.init();
        getForm().getMainComponent().validate();
        setSize(getForm().getMainComponent().getWidth(), getForm().getMainComponent().getHeight());
    }

    @NotNull
    @Override
    protected EasyConnectUrlParameterInputForm createForm() {
        return new EasyConnectUrlParameterInputForm(this, context.getInput());
    }

    private void generateCode() {
        // apply the form field values to the input
        //EasyConnectUrlParameterInputForm inputForm = getForm();
        //inputForm.applyUserInput();

        //CodeGeneratorManager manager = getCodeGenerationManager();
        //manager.generateCode(context);
    }

    @NotNull
    private CodeGeneratorManager getCodeGenerationManager() {
        return null;
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
        getOKAction().setEnabled(true);
        super.doOKAction();
    }
    
    
}
