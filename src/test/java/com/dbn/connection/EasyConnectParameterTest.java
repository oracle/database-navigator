package com.dbn.connection;

import com.dbn.connection.config.parameter.RegexConstraintValidator;
import com.dbn.connection.config.parameter.ui.UrlParameterInputForm;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class EasyConnectParameterTest {
    @Test
    public void testRetryDelayValidator() {
        RegexConstraintValidator validator = UrlParameterInputForm.RETRY_DELAY_VALIDATOR;
        // valid values return null for the ValidationInfo
        assertNull(validator.validate("fooKey", "1234"));
        assertNull(validator.validate("fooKey", "123msec"));
        assertNull(validator.validate("fooKey", "1234ms"));
        assertNull(validator.validate("fooKey", "123sec"));
        assertNull(validator.validate("fooKey", "123min"));
        assertNull(validator.validate("fooKey", "1234"));
        assertNull(validator.validate("fooKey", "1234 msec"));
        assertNull(validator.validate("fooKey", "1234 ms"));
        assertNull(validator.validate("fooKey", "1234 sec"));
        assertNull(validator.validate("fooKey", "1234 min"));

        // Invalid values return a ValidationInfo object
        assertNotNull(validator.validate("barKey", "a123ms"));
        assertNotNull(validator.validate("barKey", "123secs"));
        assertNotNull(validator.validate("barKey", "123    sec"));
    }
}
