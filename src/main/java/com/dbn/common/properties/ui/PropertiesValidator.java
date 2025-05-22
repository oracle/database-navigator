package com.dbn.common.properties.ui;

import com.intellij.openapi.ui.ValidationInfo;

public abstract class PropertiesValidator {
    public abstract ValidationInfo validate(String keyName, Object value);
}
