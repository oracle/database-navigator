package com.dbn.connection.config.provider.impl;

import com.dbn.connection.config.provider.CloudAuthenticationType;
import com.dbn.connection.config.provider.CloudConfigProviderFamily;
import com.dbn.connection.config.provider.ConfigProviderInfo;
import com.dbn.credentials.Secret;

import java.util.Map;

import static com.dbn.connection.config.provider.CloudAuthenticationType.AZURE_SERVICE_PRINCIPAL_CERTIFICATE;
import static com.dbn.connection.config.provider.CloudAuthenticationType.AZURE_SERVICE_PRINCIPAL_SECRET;

public class AzureConfigProviderHandler extends CloudConfigProviderHandlerBase {
    public static boolean isAzureClientIdAuthentication(CloudAuthenticationType authentication) {
        return authentication == CloudAuthenticationType.AZURE_INTERACTIVE ||
                isAzureServicePrincipalAuthentication(authentication);
    }

    public static boolean isAzureServicePrincipalAuthentication(CloudAuthenticationType authentication) {
        return authentication == CloudAuthenticationType.AZURE_SERVICE_PRINCIPAL_SECRET ||
                authentication == CloudAuthenticationType.AZURE_SERVICE_PRINCIPAL_CERTIFICATE;
    }

    public static void applyAuthentication(
            ConfigProviderInfo configProvider,
            CloudAuthenticationType authentication,
            String clientId,
            String tenantId,
            String certificatePath,
            char[] clientSecret,
            char[] certificatePassword) {
        configProvider.setCloudProviderAuthentication(authentication);
        configProvider.setAzureClientId(isAzureClientIdAuthentication(authentication) ? clientId : null);
        configProvider.setAzureTenantId(isAzureServicePrincipalAuthentication(authentication) ? tenantId : null);
        configProvider.setAzureClientCertificatePath(authentication == AZURE_SERVICE_PRINCIPAL_CERTIFICATE ? certificatePath : null);
        configProvider.setAzureClientSecret(authentication == AZURE_SERVICE_PRINCIPAL_SECRET ? clientSecret : Secret.EMPTY);
        configProvider.setAzureClientCertificatePassword(authentication == AZURE_SERVICE_PRINCIPAL_CERTIFICATE ? certificatePassword : Secret.EMPTY);
    }

    @Override
    public CloudConfigProviderFamily getFamily() {
        return CloudConfigProviderFamily.AZURE;
    }

    @Override
    public void addRuntimeSecrets(Map<String, String> parameters, ConfigProviderInfo configProvider) {
        switch (configProvider.getCloudProviderAuthentication()) {
            case AZURE_SERVICE_PRINCIPAL_SECRET -> addRuntimeSecret(parameters, "AZURE_CLIENT_SECRET", configProvider.getAzureClientSecret());
            case AZURE_SERVICE_PRINCIPAL_CERTIFICATE -> addRuntimeSecret(parameters, "AZURE_CLIENT_CERTIFICATE_PASSWORD", configProvider.getAzureClientCertificatePassword());
            default -> {}
        }
    }

    @Override
    public void addUrlParameters(Map<String, String> parameters, ConfigProviderInfo configProvider, boolean includeAuthentication) {
        if (configProvider.isAzureAppConfig()) {
            addParameter(parameters, "label", configProvider.getAzureAppConfigLabel());
        }

        CloudAuthenticationType authenticationType = configProvider.getCloudProviderAuthentication();
        if (authenticationType == null) return;
        if (!includeAuthentication) return;

        addParameter(parameters, "AUTHENTICATION", authenticationType.getParameterValue());

        if (isAzureClientIdAuthentication(authenticationType)) {
            addParameter(parameters, "AZURE_CLIENT_ID", configProvider.getAzureClientId());
        }

        if (isAzureServicePrincipalAuthentication(authenticationType)) {
            addParameter(parameters, "AZURE_TENANT_ID", configProvider.getAzureTenantId());

            if (authenticationType == AZURE_SERVICE_PRINCIPAL_CERTIFICATE) {
                addParameter(parameters, "AZURE_CLIENT_CERTIFICATE_PATH", configProvider.getAzureClientCertificatePath());
            }
        }
    }
}
