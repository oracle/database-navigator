package com.dbn.connection.config.provider.impl;

import com.dbn.common.util.Chars;
import com.dbn.common.util.Strings;
import com.dbn.connection.config.provider.CloudConfigProviderType;
import com.dbn.connection.config.provider.ConfigProviderInfo;
import com.dbn.connection.config.provider.ConfigSourceType;

import java.util.List;
import java.util.Map;

public abstract class CloudConfigProviderHandlerBase implements CloudConfigProviderHandler {
    @Override
    public void addUrlParameters(Map<String, String> parameters, ConfigProviderInfo configProvider, boolean includeAuthentication) {
    }

    @Override
    public boolean managesProviderLocation(ConfigSourceType sourceType, CloudConfigProviderType providerType) {
        return false;
    }

    @Override
    public void setProviderLocation(ConfigProviderInfo configProvider, String providerLocation) {
    }

    @Override
    public String getProviderLocation(ConfigProviderInfo configProvider) {
        return null;
    }

    @Override
    public void initialize(ConfigProviderInfo configProvider, Map<String, String> parameters) {
        configProvider.setProviderProfileKey(getParameter(parameters, "key"));
    }

    @Override
    public void validate(ConfigProviderInfo configProvider, List<String> errors) {
    }

    protected static void addRuntimeSecret(Map<String, String> parameters, String parameterName, char[] value) {
        if (!Chars.isNotEmpty(value)) return;

        parameters.put(parameterName, Chars.toString(value));
    }

    protected static void addParameter(Map<String, String> parameters, String parameterName, String value) {
        if (Strings.isEmptyOrSpaces(value)) return;
        parameters.put(parameterName, value.trim());
    }

    protected static String getParameter(Map<String, String> parameters, String parameterName) {
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(parameterName)) return entry.getValue();
        }
        return null;
    }
}
