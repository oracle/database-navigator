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
import com.dbn.connection.config.parameter.RegexConstraintValidator;
import com.dbn.connection.config.parameter.StringListConstraintValidator;
import lombok.Getter;

import java.awt.Dimension;
import java.util.Map;
import static com.dbn.connection.config.parameter.IntegerConstraintValidator.MUST_BE_ZERO_OR_MORE;

import static com.dbn.connection.config.ui.ConnectionUrlSettingsForm.EASY_CONNECT_BOOLEAN_LIKE_STRING_VALUES;

@Getter
public class UrlParameterInputForm extends PropertiesEditorForm {
    public static final String RETRY_DELAY_SHOULD_MATCH = "80, 80ms, 80sec, or 80min. Default is sec if unit is not specified";
    public final static RegexConstraintValidator.ValidationPattern RETRY_DELAY_PATTERN =
            new RegexConstraintValidator.ValidationPattern("\\d+( )?(ms|msec|sec|min)?", RETRY_DELAY_SHOULD_MATCH);
    public final static RegexConstraintValidator RETRY_DELAY_VALIDATOR = new RegexConstraintValidator(RETRY_DELAY_PATTERN);

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
        addValidator(new StringListConstraintValidator(EASY_CONNECT_BOOLEAN_LIKE_STRING_VALUES), "FAILOVER");
        addValidator(new StringListConstraintValidator(EASY_CONNECT_BOOLEAN_LIKE_STRING_VALUES), "LOAD_BALANCE");
        addValidator(MUST_BE_ZERO_OR_MORE, "SDU");
        addValidator(new StringListConstraintValidator("on", "off", "yes", "no"), "SOURCE_ROUTE");
        addValidator(MUST_BE_ZERO_OR_MORE, "RETRY_COUNT");
        addValidator(RETRY_DELAY_VALIDATOR, "RETRY_DELAY");
    }
}
