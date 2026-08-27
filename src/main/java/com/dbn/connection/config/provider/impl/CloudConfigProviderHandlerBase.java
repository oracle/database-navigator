package com.dbn.connection.config.provider.impl;

import com.dbn.common.util.Chars;
import com.dbn.common.util.Strings;
import com.dbn.connection.config.provider.ConfigProviderInfo;

import java.util.Map;

public abstract class CloudConfigProviderHandlerBase implements CloudConfigProviderHandler {
    @Override
    public void addUrlParameters(Map<String, String> parameters, ConfigProviderInfo configProvider, boolean includeAuthentication) {
    }

    protected static void addRuntimeSecret(Map<String, String> parameters, String parameterName, char[] value) {
        if (!Chars.isNotEmpty(value)) return;

        parameters.put(parameterName, Chars.toString(value));
    }

    protected static void addParameter(Map<String, String> parameters, String parameterName, String value) {
        if (Strings.isEmptyOrSpaces(value)) return;
        parameters.put(parameterName, value.trim());
    }
}
