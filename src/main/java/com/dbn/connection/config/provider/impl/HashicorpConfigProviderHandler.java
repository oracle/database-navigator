package com.dbn.connection.config.provider.impl;

import com.dbn.connection.config.provider.CloudAuthenticationType;
import com.dbn.connection.config.provider.CloudConfigProviderFamily;
import com.dbn.connection.config.provider.ConfigProviderInfo;
import com.dbn.credentials.Secret;

import java.util.Map;

import static com.dbn.connection.config.provider.CloudAuthenticationType.HCP_APPROLE;
import static com.dbn.connection.config.provider.CloudAuthenticationType.HCP_DEFAULT;
import static com.dbn.connection.config.provider.CloudAuthenticationType.HCP_GITHUB;
import static com.dbn.connection.config.provider.CloudAuthenticationType.HCP_USERPASS;
import static com.dbn.connection.config.provider.CloudAuthenticationType.HCP_VAULT_TOKEN;


public class HashicorpConfigProviderHandler extends CloudConfigProviderHandlerBase {
    public static void applyAuthentication(
            ConfigProviderInfo configProvider,
            CloudAuthenticationType authentication,
            String vaultAddress,
            String vaultNamespace,
            String vaultUsername,
            String userPassAuthPath,
            String appRoleId,
            String appRoleAuthPath,
            String githubAuthPath,
            char[] vaultToken,
            char[] vaultPassword,
            char[] appRoleSecretId,
            char[] githubToken) {
        configProvider.setCloudProviderAuthentication(authentication);
        configProvider.setHashicorpVaultAddress(vaultAddress);
        configProvider.setHashicorpVaultNamespace(vaultNamespace);
        configProvider.setHashicorpVaultUsername(authentication == HCP_USERPASS ? vaultUsername : null);
        configProvider.setHashicorpUserpassAuthPath(authentication == HCP_USERPASS ? userPassAuthPath : null);
        configProvider.setHashicorpAppRoleId(authentication == HCP_APPROLE ? appRoleId : null);
        configProvider.setHashicorpAppRoleAuthPath(authentication == HCP_APPROLE ? appRoleAuthPath : null);
        configProvider.setHashicorpGithubAuthPath(authentication == HCP_GITHUB ? githubAuthPath : null);

        configProvider.setHashicorpVaultToken(authentication == HCP_VAULT_TOKEN ? vaultToken : Secret.EMPTY);
        configProvider.setHashicorpVaultPassword(authentication == HCP_USERPASS ? vaultPassword : Secret.EMPTY);
        configProvider.setHashicorpAppRoleSecretId(authentication == HCP_APPROLE ? appRoleSecretId : Secret.EMPTY);
        configProvider.setHashicorpGithubToken(authentication == HCP_GITHUB ? githubToken : Secret.EMPTY);
    }

    @Override
    public CloudConfigProviderFamily getFamily() {
        return CloudConfigProviderFamily.HASHICORP;
    }

    @Override
    public void addRuntimeSecrets(Map<String, String> parameters, ConfigProviderInfo configProvider) {
        switch (configProvider.getCloudProviderAuthentication()) {
            case HCP_VAULT_TOKEN -> addRuntimeSecret(parameters, "VAULT_TOKEN", configProvider.getHashicorpVaultToken());
            case HCP_USERPASS -> addRuntimeSecret(parameters, "VAULT_PASSWORD", configProvider.getHashicorpVaultPassword());
            case HCP_APPROLE -> addRuntimeSecret(parameters, "SECRET_ID", configProvider.getHashicorpAppRoleSecretId());
            case HCP_GITHUB -> addRuntimeSecret(parameters, "GITHUB_TOKEN", configProvider.getHashicorpGithubToken());
            default -> {}
        }
    }

    @Override
    public void addUrlParameters(Map<String, String> parameters, ConfigProviderInfo configProvider, boolean includeAuthentication) {
        CloudAuthenticationType authenticationType = configProvider.getCloudProviderAuthentication();

        if (includeAuthentication && authenticationType != null && authenticationType != HCP_DEFAULT) {
            addParameter(parameters, "AUTHENTICATION", authenticationType.getParameterValue().toUpperCase());
        }

        addParameter(parameters, "VAULT_ADDR", configProvider.getHashicorpVaultAddress());
        addParameter(parameters, "VAULT_NAMESPACE", configProvider.getHashicorpVaultNamespace());

        if (authenticationType == HCP_USERPASS) {
            addParameter(parameters, "VAULT_USERNAME", configProvider.getHashicorpVaultUsername());
            addParameter(parameters, "USERPASS_AUTH_PATH", configProvider.getHashicorpUserpassAuthPath());

        } else if (authenticationType == HCP_APPROLE) {
            addParameter(parameters, "ROLE_ID", configProvider.getHashicorpAppRoleId());
            addParameter(parameters, "APPROLE_AUTH_PATH", configProvider.getHashicorpAppRoleAuthPath());

        } else if (authenticationType == HCP_GITHUB) {
            addParameter(parameters, "GITHUB_AUTH_PATH", configProvider.getHashicorpGithubAuthPath());
        }
    }
}
