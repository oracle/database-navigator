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

package com.dbn.connection.config.parameter.ui;

import com.dbn.common.properties.ui.PropertiesEditorForm;
import com.dbn.common.properties.ui.PropertiesTableModel;
import com.dbn.connection.config.parameter.IntegerConstraintValidator;
import com.dbn.connection.config.parameter.StringListConstraintValidator;
import lombok.Getter;

import java.awt.Dimension;
import java.util.Map;

@Getter
public class UrlParameterInputForm extends PropertiesEditorForm {
    public UrlParameterInputForm(UrlParameterInputDialog dialog, Map<String, String> parameters) {
        super(dialog, parameters, false, false);
        initPropertyTable();
        initPropertyValidators();
    }

    private void initPropertyTable() {
        PropertiesTableModel model = getTable().getModel();
        model.setPredefinedPropertySet(true);

        int height = getTable().getPreferredSize().height;
        getMainComponent().setPreferredSize(new Dimension(480, height));
    }

    protected void initPropertyValidators() {
        // TODO make more generic to support url parameters for connections other than EZ_CONNECT

        addValidator(new IntegerConstraintValidator(0), "SEND_BUF_SIZE");
        addValidator(new IntegerConstraintValidator(0), "RECV_BUF_SIZE");
        addValidator(new StringListConstraintValidator("ON", "OFF"), "FAILOVER");
        addValidator(new StringListConstraintValidator("ON", "OFF"), "LOAD_BALANCE");
        addValidator(new IntegerConstraintValidator(0), "SDU");
        addValidator(new IntegerConstraintValidator(0), "SDU");
        addValidator(new StringListConstraintValidator("ON", "OFF"), "SOURCE_ROUTE");
        addValidator(new IntegerConstraintValidator(0), "RETRY_COUNT");
        addValidator(new IntegerConstraintValidator(0), "RETRY_DELAY");

        addValidator(new StringListConstraintValidator(
                "ON", "OFF", "on", "off", "YES","NO","yes", "no", "TRUE", "FALSE", "true", "false"), "SSL_SERVER_DN_MATCH");
    }
}
