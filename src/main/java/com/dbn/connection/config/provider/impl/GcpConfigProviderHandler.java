package com.dbn.connection.config.provider.impl;

import com.dbn.connection.config.provider.CloudConfigProviderFamily;
import com.dbn.connection.config.provider.CloudConfigProviderType;
import com.dbn.connection.config.provider.ConfigProviderInfo;
import com.dbn.connection.config.provider.ConfigSourceType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Strings.isEmptyOrSpaces;

public class GcpConfigProviderHandler extends CloudConfigProviderHandlerBase {
    @Override
    public boolean managesProviderLocation(ConfigSourceType sourceType, CloudConfigProviderType providerType) {
        return sourceType == ConfigSourceType.CLOUD && providerType == CloudConfigProviderType.GCP_STORAGE;
    }

    @Override
    public void setProviderLocation(ConfigProviderInfo configProvider, String providerLocation) {
        Map<String, String> locationParameters = parseLocation(providerLocation);
        configProvider.setGcpStorageProject(locationParameters.get("project"));
        configProvider.setGcpStorageBucket(locationParameters.get("bucket"));
        configProvider.setGcpStorageObject(locationParameters.get("object"));
    }

    @Override
    public String getProviderLocation(ConfigProviderInfo configProvider) {
        return getStorageLocation(
                configProvider.getGcpStorageProject(),
                configProvider.getGcpStorageBucket(),
                configProvider.getGcpStorageObject());
    }

    public static String getStorageLocation(String project, String bucket, String object) {
        if (isEmptyOrSpaces(project) && isEmptyOrSpaces(bucket) && isEmptyOrSpaces(object)) return "";

        return "project=" + nvl(project, "").trim() +
                ";bucket=" + nvl(bucket, "").trim() +
                ";object=" + nvl(object, "").trim();
    }

    private static Map<String, String> parseLocation(String location) {
        Map<String, String> parameters = new HashMap<>();
        if (isEmptyOrSpaces(location)) return parameters;

        for (String token : location.split(";")) {
            String[] entry = token.split("=", 2);
            if (entry.length == 2) {
                parameters.put(entry[0].trim().toLowerCase(), entry[1].trim());
            }
        }
        return parameters;
    }

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
