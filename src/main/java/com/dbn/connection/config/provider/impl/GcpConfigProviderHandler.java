package com.dbn.connection.config.provider.impl;

import com.dbn.connection.config.provider.CloudConfigProviderFamily;
import com.dbn.connection.config.provider.CloudConfigProviderType;
import com.dbn.connection.config.provider.ConfigProviderInfo;

import java.util.List;
import java.util.Map;

import static com.dbn.common.util.Strings.isEmptyOrSpaces;

public class GcpConfigProviderHandler extends CloudConfigProviderHandlerBase {
    @Override
    public void validate(ConfigProviderInfo configProvider, List<String> errors) {
        if (configProvider.getCloudProviderType() != CloudConfigProviderType.GCP_STORAGE) return;
        if (isEmptyOrSpaces(configProvider.getGcpStorageProject()) ||
                isEmptyOrSpaces(configProvider.getGcpStorageBucket()) ||
                isEmptyOrSpaces(configProvider.getGcpStorageObject())) {
            errors.add("GCP Cloud Storage config location requires project, bucket and object");
        }
    }

    public CloudConfigProviderFamily getFamily() {
        return CloudConfigProviderFamily.GCP;
    }

    public void addRuntimeSecrets(Map<String, String> parameters, ConfigProviderInfo configProvider) {}
}
