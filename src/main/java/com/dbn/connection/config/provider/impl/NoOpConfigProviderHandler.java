package com.dbn.connection.config.provider.impl;

import com.dbn.connection.config.provider.CloudConfigProviderFamily;
import com.dbn.connection.config.provider.ConfigProviderInfo;

import java.util.Map;

public class NoOpConfigProviderHandler extends CloudConfigProviderHandlerBase {
    public static final NoOpConfigProviderHandler INSTANCE = new NoOpConfigProviderHandler();

    @Override
    public CloudConfigProviderFamily getFamily() {
        return null;
    }

    @Override
    public void addRuntimeSecrets(Map<String, String> parameters, ConfigProviderInfo configProvider) {
    }
}
