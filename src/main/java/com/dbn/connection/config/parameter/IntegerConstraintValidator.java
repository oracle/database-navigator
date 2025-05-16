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

public class IntegerConstraintValidator extends PropertiesValidator {
    private final int lowerBound;
    private final int upperBound;

    public static final IntegerConstraintValidator MUST_BE_ZERO_OR_MORE = new IntegerConstraintValidator(0);
    public IntegerConstraintValidator(int lowerBound) {
        this(lowerBound, Integer.MAX_VALUE);
    }

    public IntegerConstraintValidator(int lowerBound, int upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    @Override
    public ValidationInfo validate(String keyName, Object value) {
        int intValue;
        if (value instanceof String) {
            try {
                intValue = Integer.parseInt((String) value);
            } catch (NumberFormatException nfe) {
                return new ValidationInfo(keyName + " must be an integer");
            }
        }
        else if (value instanceof Integer) {
            intValue = (Integer) value;
        }
        else {
            return new ValidationInfo(keyName + " must be an integer");
        }

        if (intValue < lowerBound) {
            return new ValidationInfo(keyName + " must be an integer value of at least "+lowerBound);
        }
        else if (intValue > upperBound ) {
            return new ValidationInfo(keyName + " mut be an integer value of no more than "+upperBound);
        }
        return null;
    }
}
