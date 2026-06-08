package com.dbn.connection.config.parameter;

import com.dbn.common.properties.ui.PropertiesValidator;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

import static com.dbn.nls.NlsResources.txt;

public class RegexConstraintValidator extends PropertiesValidator {

    private final ValidationPattern validationPattern;

    public static class ValidationPattern {
        private final Pattern pattern;
        private final String matchMessage;

        public ValidationPattern(Pattern pattern, String matchMessage) {
            this.pattern = pattern;
            this.matchMessage = matchMessage;
        }
        public ValidationPattern(@NotNull String pattern, String matchMessage) {
            this(Pattern.compile(pattern), matchMessage);
        }
        public String match(Object target) {
            if (!(target instanceof String)) {
                return matchMessage;
            }
            if (!this.pattern.matcher((String) target).matches()) {
                return matchMessage;
            }
            return null;
        }

    }

    public RegexConstraintValidator(ValidationPattern validationPattern) {
        this.validationPattern = validationPattern;
    }
    @Override
    public ValidationInfo validate(String keyName, Object value) {
        if (! (value instanceof String)) {
            return new ValidationInfo(txt("cfg.connection.error.ParameterValueMustBeString", keyName));
        }
        String message = this.validationPattern.match(value);
        if (message != null) {
            return new ValidationInfo(txt("cfg.connection.error.ParameterValueMustMatch", keyName, message, value));
        }
        return null;
    }
}
