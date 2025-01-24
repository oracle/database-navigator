package com.dbn.connection.config.ui.easyconn;

import com.dbn.common.properties.ui.PropertiesValidator;
import com.intellij.openapi.ui.ValidationInfo;
import it.unimi.dsi.fastutil.bytes.V;

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
            return new ValidationInfo(keyName + " must be one of " + allowedStrings.toString() + " but was "+value.toString());
        }
        return null;
    }
}
