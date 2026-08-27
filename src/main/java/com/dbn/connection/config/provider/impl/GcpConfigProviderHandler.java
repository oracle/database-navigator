package com.dbn.connection.config.provider.impl;

import com.dbn.connection.config.provider.CloudConfigProviderFamily;
import com.dbn.connection.config.provider.ConfigProviderInfo;

import java.util.Map;

public class GcpConfigProviderHandler extends CloudConfigProviderHandlerBase {
    public CloudConfigProviderFamily getFamily() {
        return CloudConfigProviderFamily.GCP;
    }

    public void addRuntimeSecrets(Map<String, String> parameters, ConfigProviderInfo configProvider) {}
}
