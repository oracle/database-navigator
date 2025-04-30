package com.dbn.connection.config.parameter;

import com.dbn.common.properties.ui.PropertiesValidator;
import com.intellij.openapi.ui.ValidationInfo;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
/**
 * A property validator that generates errors if invalid characters
 * appear in a String property. An optional preprocessor function may
 * be provided to fix the string before validation.  Commonly this is
 * to remove sorrounding quotes before quotes are validated in the rest.
 */
public class CheckForInvalidCharactersValidator extends PropertiesValidator {
    private final Set<Character> invalidChars;
    private final Optional<Function<String, String>> preprocessor;

    /**
     *
     * @param invalidChars Set of characters to vaild for
     * @param preprocessor A preprocessor function ta will be called on the input
     *                     String before validation if present.
     */
    public CheckForInvalidCharactersValidator(Set<Character> invalidChars, Optional<Function<String,String>> preprocessor) {
        super();
        this.invalidChars = invalidChars;
        this.preprocessor = preprocessor;
    }

    /**
     *
     * @param invalidChars Set of characters to vaild for
     */
    public CheckForInvalidCharactersValidator(Set<Character> invalidChars) {
        this(invalidChars, Optional.empty());
    }
    @Override
    public ValidationInfo validate(String keyName, Object value) {
        if (!(value instanceof String)) {
            return new ValidationInfo("Value must be a String");
        }
        MutableObject<String> strVal = new MutableObject<>((String)value);
        preprocessor.ifPresent(p -> {
            strVal.setValue(p.apply(strVal.getValue()));
        });

        for (Character invalidChar : invalidChars) {
            if (strVal.getValue().indexOf(invalidChar.charValue()) > -1) {
                return new ValidationInfo("Value must not contain '"+invalidChar+"'");
            }
        }
        return null;
    }
}
