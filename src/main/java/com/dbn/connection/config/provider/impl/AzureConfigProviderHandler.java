package com.dbn.connection.config.provider.impl;

import com.dbn.connection.config.provider.CloudAuthenticationType;
import com.dbn.connection.config.provider.CloudConfigProviderFamily;
import com.dbn.connection.config.provider.ConfigProviderInfo;
import com.dbn.credentials.Secret;

import java.util.List;
import java.util.Map;

import static com.dbn.common.util.Strings.isEmptyOrSpaces;
import static com.dbn.connection.config.provider.CloudAuthenticationType.AZURE_SERVICE_PRINCIPAL_CERTIFICATE;
import static com.dbn.connection.config.provider.CloudAuthenticationType.AZURE_SERVICE_PRINCIPAL_SECRET;

public class AzureConfigProviderHandler extends CloudConfigProviderHandlerBase {
    @Override
    public void validate(ConfigProviderInfo configProvider, List<String> errors) {
        CloudAuthenticationType authenticationType = configProvider.getCloudAuthenticationType();
        if (authenticationType == CloudAuthenticationType.AZURE_INTERACTIVE && isEmptyOrSpaces(configProvider.getAzureClientId())) {
            errors.add("Azure interactive authentication requires client ID");
        }
        if (isAzureServicePrincipalAuthentication(authenticationType)) {
            if (isEmptyOrSpaces(configProvider.getAzureClientId())) {
                errors.add("Azure service principal authentication requires client ID");
            }
            if (isEmptyOrSpaces(configProvider.getAzureTenantId())) {
                errors.add("Azure service principal authentication requires tenant ID");
            }
            if (authenticationType == AZURE_SERVICE_PRINCIPAL_CERTIFICATE && isEmptyOrSpaces(configProvider.getAzureClientCertificatePath())) {
                errors.add("Azure service principal certificate authentication requires certificate path");
            }
        }
    }

    @Override
    public void initialize(ConfigProviderInfo configProvider, Map<String, String> parameters) {
        super.initialize(configProvider, parameters);
        configProvider.setAzureAppConfigLabel(configProvider.isAzureAppConfig() ? getParameter(parameters, "label") : null);
        String certificatePath = getParameter(parameters, "AZURE_CLIENT_CERTIFICATE_PATH");
        configProvider.setCloudAuthenticationType(CloudAuthenticationType.getAzure(
                getParameter(parameters, "AUTHENTICATION"),
                com.dbn.common.util.Strings.isNotEmptyOrSpaces(certificatePath)));
        configProvider.setAzureClientId(getParameter(parameters, "AZURE_CLIENT_ID"));
        configProvider.setAzureTenantId(getParameter(parameters, "AZURE_TENANT_ID"));
        configProvider.setAzureClientCertificatePath(certificatePath);
    }

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
        configProvider.setCloudAuthenticationType(authentication);
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
        switch (configProvider.getCloudAuthenticationType()) {
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

        CloudAuthenticationType authenticationType = configProvider.getCloudAuthenticationType();
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
