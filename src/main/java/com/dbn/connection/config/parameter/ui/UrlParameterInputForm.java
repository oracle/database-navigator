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
import com.dbn.connection.config.parameter.StringListConstraintValidator;
import lombok.Getter;

import java.awt.Dimension;
import java.util.Map;

import static com.dbn.connection.config.EasyConnectParameters.BOOLEAN_LIKE_STRING_VALUES;
import static com.dbn.connection.config.EasyConnectParameters.NO_DQUOTES_ALLOWED_IN_PROPERTY;
import static com.dbn.connection.config.EasyConnectParameters.RETRY_DELAY_VALIDATOR;
import static com.dbn.connection.config.parameter.IntegerConstraintValidator.MUST_BE_ZERO_OR_MORE;

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

        addValidator(MUST_BE_ZERO_OR_MORE, "SEND_BUF_SIZE");
        addValidator(MUST_BE_ZERO_OR_MORE, "RECV_BUF_SIZE");
        addValidator(new StringListConstraintValidator(BOOLEAN_LIKE_STRING_VALUES), "FAILOVER");
        addValidator(new StringListConstraintValidator(BOOLEAN_LIKE_STRING_VALUES), "LOAD_BALANCE");
        addValidator(MUST_BE_ZERO_OR_MORE, "SDU");
        addValidator(new StringListConstraintValidator("ON", "OFF", "on", "off", "YES", "NO", "yes", "no"), "SOURCE_ROUTE");
        addValidator(MUST_BE_ZERO_OR_MORE, "RETRY_COUNT");
        addValidator(RETRY_DELAY_VALIDATOR, "RETRY_DELAY");

        addValidator(new StringListConstraintValidator(
                "ON", "OFF", "on", "off", "YES","NO","yes", "no", "TRUE", "FALSE", "true", "false"), "SSL_SERVER_DN_MATCH");
        addValidator(NO_DQUOTES_ALLOWED_IN_PROPERTY,"SSL_SERVER_CERT_DN");
        addValidator(NO_DQUOTES_ALLOWED_IN_PROPERTY, "WALLET_LOCATION");

    }
}
