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

import com.dbn.common.properties.ui.PropertiesEditorForm;
import com.dbn.common.properties.ui.PropertiesTableModel;
import com.dbn.generator.code.shared.CodeGeneratorInput;
import lombok.Getter;

@Getter
public class EasyConnectUrlParameterInputForm<I extends EasyConnectUrlParameterInput> extends PropertiesEditorForm {
    private final I input;
    public EasyConnectUrlParameterInputForm(EasyConnectUrlParameterInputDialog dialog, I input) {
        super(dialog, input.getExistingParameterValues(), false, false);
        this.input = input;
        initPropertyTable();
        initPropertyValidators();
    }

    private void initPropertyTable() {
        PropertiesTableModel model = getTable().getModel();
        model.setPredefinedPropertySet(true);
        model.addTableModelListener(e -> applyUserInput());
    }

    protected void initPropertyValidators() {
        /*("ENABLE", ,
               "HTTPS_PROXY") */
        addValidator(new IntegerConstraintValidator(0), "SEND_BUF_SIZE");
        addValidator(new IntegerConstraintValidator(0), "RECV_BUF_SIZE");
        addValidator(new StringListConstraintValidator("ON", "OFF"), "FAILOVER");
        addValidator(new StringListConstraintValidator("ON", "OFF"), "LOAD_BALANCE");
        addValidator(new IntegerConstraintValidator(0), "SDU");
        addValidator(new IntegerConstraintValidator(0), "SDU");
        addValidator(new StringListConstraintValidator("ON", "OFF"), "SOURCE_ROUTE");
        addValidator(new IntegerConstraintValidator(0), "RETRY_COUNT");
        addValidator(new IntegerConstraintValidator(0), "RETRY_DELAY");

        //?/propertiesEditorForm.addValidator(new );
    }

    public final void applyUserInput() {
        applyUserInput(input);
    }

    /**
     * Expected to apply all user inputs from the input-form fields to the {@link CodeGeneratorInput}
     * @param input the
     */
    protected void applyUserInput(I input) {
        input.setParameters(getProperties());
    }
}
