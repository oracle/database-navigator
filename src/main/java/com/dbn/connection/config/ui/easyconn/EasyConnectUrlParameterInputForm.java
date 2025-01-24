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
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.generator.code.shared.CodeGeneratorInput;
import lombok.Getter;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.util.Map;

@Getter
public class EasyConnectUrlParameterInputForm<I extends EasyConnectUrlParameterInput> extends DBNFormBase {
    private final I input;
    private PropertiesEditorForm propertiesEditorForm;
    private JPanel mainPanel;
    public EasyConnectUrlParameterInputForm(EasyConnectUrlParameterInputDialog dialog, I input) {
        super(dialog);
        this.input = input;
        initMainPanel();
        initValidation();;
    }

    private void initValidation() {
        /*("ENABLE", ,
               "HTTPS_PROXY") */
        propertiesEditorForm.addValidator(new IntegerConstraintValidator(0), "SEND_BUF_SIZE");
        propertiesEditorForm.addValidator(new IntegerConstraintValidator(0), "RECV_BUF_SIZE");
        propertiesEditorForm.addValidator(new StringListConstraintValidator("ON", "OFF"), "FAILOVER");
        propertiesEditorForm.addValidator(new StringListConstraintValidator("ON", "OFF"), "LOAD_BALANCE");
        propertiesEditorForm.addValidator(new IntegerConstraintValidator(0), "SDU");
        propertiesEditorForm.addValidator(new IntegerConstraintValidator(0), "SDU");
        propertiesEditorForm.addValidator(new StringListConstraintValidator("ON", "OFF"), "SOURCE_ROUTE");
        propertiesEditorForm.addValidator(new IntegerConstraintValidator(0), "RETRY_COUNT");
        propertiesEditorForm.addValidator(new IntegerConstraintValidator(0), "RETRY_DELAY");

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
        input.setParameters(this.propertiesEditorForm.getProperties());
    }

    protected JPanel initMainPanel() {
        this.mainPanel = new JPanel();
        GridLayout glayout = new GridLayout();
        glayout.setRows(1);
        glayout.setColumns(1);
        mainPanel.setLayout(glayout);
        //JLabel propsLbl = new JLabel("Parameters");
        //mainPanel.add(propsLbl);

        Map<String, String> props = this.input.getExistingParameterValues();
        this.propertiesEditorForm =
                new PropertiesEditorForm(this, props, false, false);
        propertiesEditorForm.getTable().getModel().setPropertyColumnReadonly(true);
        propertiesEditorForm.getTable().getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                applyUserInput();
            }
        });

        mainPanel.add(propertiesEditorForm.getComponent());
        return mainPanel;
    }

    @Override
    protected JComponent getMainComponent() {
        return this.mainPanel;
    }
}
