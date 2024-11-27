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

package com.dbn.common.options.ui;

import com.dbn.common.util.Strings;
import com.intellij.openapi.options.ConfigurationException;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JTextField;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

// TODO NLS (usages of this)
@UtilityClass
public class ConfigurationEditors {
    public static int validateIntegerValue(@NotNull JTextField inputField, @NotNull String name, boolean required, int min, int max, @Nullable String hint) throws ConfigurationException {
        try {

            String value = inputField.getText();
            if (required && Strings.isEmpty(value)) {
                String message = txt("cfg.shared.error.MissingInputValue", name);
                throw new ConfigurationException(message, txt("cfg.shared.title.InvalidConfigValue"));
            }

            if (Strings.isNotEmpty(value)) {
                int integer = Integer.parseInt(value);
                if (min > integer || max < integer) throw new NumberFormatException("Number not in range");
                return integer;
            }
            return 0;
        } catch (NumberFormatException e) {
            conditionallyLog(e);
            inputField.grabFocus();
            inputField.selectAll();
            String message = txt("cfg.shared.error.InputValueNotInRange", name, min, max);
            if (hint != null) {
                message = message + " " + hint;
            }
            throw new ConfigurationException(message, txt("cfg.shared.title.InvalidConfigValue"));
        }
    }

    public static String validateStringValue(@NotNull JTextField inputField, @NotNull String name, boolean required) throws ConfigurationException {
        String value = inputField.getText().trim();
        if (required && value.isEmpty()) {
            String message = txt("cfg.shared.error.MissingInputValue", name);
            throw new ConfigurationException(message, txt("cfg.shared.title.InvalidConfigValue"));
        }
        return value;
    }
    
}
