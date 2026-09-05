package com.dbn.connection.config.provider.impl;

import com.dbn.connection.config.provider.CloudConfigProviderFamily;
import com.dbn.connection.config.provider.ConfigProviderInfo;

import java.util.Map;


public class AwsConfigProviderHandler extends CloudConfigProviderHandlerBase {
    @Override
    public void initialize(ConfigProviderInfo configProvider, Map<String, String> parameters) {
        super.initialize(configProvider, parameters);
        configProvider.setAwsRegion(getParameter(parameters, configProvider.getCloudProviderType().getAwsRegionParameterName()));
    }

    public CloudConfigProviderFamily getFamily() {
        return CloudConfigProviderFamily.AWS;
    }

    public void addRuntimeSecrets(Map<String, String> parameters, ConfigProviderInfo configProvider) {
    }

    public void addUrlParameters(Map<String, String> parameters, ConfigProviderInfo configProvider, boolean includeAuthentication) {
        if (configProvider.isAwsRegionConfig()) {
            addParameter(parameters, "AWS_REGION", configProvider.getAwsRegion());
        }
    }
}
