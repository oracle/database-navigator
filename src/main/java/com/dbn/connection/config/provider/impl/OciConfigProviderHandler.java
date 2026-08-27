package com.dbn.connection.config.provider.impl;

import com.dbn.connection.config.provider.CloudAuthenticationType;
import com.dbn.connection.config.provider.CloudConfigProviderFamily;
import com.dbn.connection.config.provider.ConfigProviderInfo;

import java.util.LinkedHashMap;
import java.util.Map;


public class OciConfigProviderHandler extends CloudConfigProviderHandlerBase {
    public static void applyAuthentication(
        ConfigProviderInfo configProvider,
            CloudAuthenticationType authentication,
            String configFile,
            String profile) {
        configProvider.setCloudProviderAuthentication(authentication);
        configProvider.setOciConfigFile(authentication == CloudAuthenticationType.OCI_DEFAULT ? configFile : null);
        configProvider.setOciConfigProfile(authentication == CloudAuthenticationType.OCI_DEFAULT ? profile : null);
    }

    public CloudConfigProviderFamily getFamily() {
        return CloudConfigProviderFamily.OCI;
    }

    public void addRuntimeSecrets(Map<String, String> parameters, ConfigProviderInfo configProvider) {}

    public void addUrlParameters(Map<String, String> parameters, ConfigProviderInfo configProvider, boolean includeAuthentication) {
        if (includeAuthentication) {
            CloudAuthenticationType authenticationType = configProvider.getCloudProviderAuthentication();
            String ociConfigFile = configProvider.getOciConfigFile();
            String ociConfigProfile = configProvider.getOciConfigProfile();

            Map<String, String> param = buildParameters(authenticationType, ociConfigFile, ociConfigProfile);
            parameters.putAll(param);
        }
    }


    public static Map<String, String> buildParameters(
            CloudAuthenticationType authentication,
            String configFile,
            String profile) {
        Map<String, String> parameters = new LinkedHashMap<>();
        if (authentication == null) return parameters;

        addParameter(parameters, "AUTHENTICATION", authentication.getParameterValue());

        if (authentication != CloudAuthenticationType.OCI_DEFAULT) {
            return parameters;
        }

        addParameter(parameters, "OCI_CONFIG_FILE", configFile);
        addParameter(parameters, "OCI_PROFILE", profile);

        return parameters;
    }
}
