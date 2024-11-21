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

package com.dbn.common.options.setting;

import com.intellij.openapi.options.ConfigurationException;

import javax.swing.JTextField;

public class IntegerSettingValidator implements SettingValidator<IntegerSetting>{
    private String fieldName;
    private String hint;
    private int minValue;
    private int maxValue;
    
    @Override
    public void validate(IntegerSetting setting) throws ConfigurationException {
        
    }

    public static int parseIntegerInputValue(JTextField inputField, String name, int min, int max, String hint) throws ConfigurationException {
        try {
            int integer = Integer.parseInt(inputField.getText());
            if (min > integer || max < integer) throw new NumberFormatException("Number not in range");
            return integer;
        } catch (NumberFormatException e) {
            inputField.grabFocus();
            inputField.selectAll();
            String message = "Input value for \"" + name + "\" must be an integer between " + min + " and " + max + ".";
            if (hint != null) {
                message = message + " " + hint;
            }
            throw new ConfigurationException(message, "Invalid config value");
        }
    }
}
