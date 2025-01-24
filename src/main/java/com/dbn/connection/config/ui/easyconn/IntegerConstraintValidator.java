package com.dbn.connection.config.ui.easyconn;

import com.dbn.common.properties.ui.PropertiesValidator;
import com.intellij.openapi.ui.ValidationInfo;

public class IntegerConstraintValidator extends PropertiesValidator {
    private final int lowerBound;
    private final int upperBound;

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
                intValue = Integer.valueOf((String) value);
            } catch (NumberFormatException nfe) {
                return new ValidationInfo(keyName + " must be an integer");
            }
        }
        else if (value instanceof Integer) {
            intValue = ((Integer)value).intValue();
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
