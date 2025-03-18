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

package com.dbn.connection.config.parameter;

import com.dbn.common.properties.ui.PropertiesValidator;
import com.intellij.openapi.ui.ValidationInfo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class StringListConstraintValidator extends PropertiesValidator {

    private final Set<String> allowedStrings = new HashSet<>();
    public StringListConstraintValidator(String...strList) {
        super();
        allowedStrings.addAll(Arrays.asList(strList));
    }

    @Override
    public ValidationInfo validate(String keyName, Object value) {
        if (! (value instanceof String)) {
            return new ValidationInfo(keyName + " must be a string");
        }
        if (!allowedStrings.contains((String) value)) {
            return new ValidationInfo(keyName + " must be one of " + allowedStrings + " but was " + value);
        }
        return null;
    }
}
