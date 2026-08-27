/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.connection.config.provider;

import com.dbn.common.options.BasicConfiguration;
import com.dbn.common.options.PersistentConfiguration;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.util.Cloneable;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Strings;
import com.dbn.connection.DatabaseUrlPattern;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.OciConfigProviderParameters;
import com.dbn.credentials.Secret;
import com.dbn.credentials.SecretsOwner;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.options.setting.Settings.getEnum;
import static com.dbn.common.options.setting.Settings.getString;
import static com.dbn.common.options.setting.Settings.setEnum;
import static com.dbn.common.options.setting.Settings.setString;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Strings.isEmptyOrSpaces;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.connection.DatabaseUrlType.PROVIDER;
import static com.dbn.connection.config.provider.CloudAuthenticationType.AZURE_INTERACTIVE;
import static com.dbn.connection.config.provider.CloudAuthenticationType.OCI_INTERACTIVE;
import static com.dbn.connection.config.provider.CloudConfigProviderType.AZURE_APP_CONFIG;
import static com.dbn.credentials.SecretType.CONNECTION_AZURE_CONFIG_PROVIDER_CERTIFICATE_PASSWORD;
import static com.dbn.credentials.SecretType.CONNECTION_AZURE_CONFIG_PROVIDER_CLIENT_SECRET;
import static com.dbn.credentials.SecretType.CONNECTION_HASHICORP_APPROLE_SECRET_ID;
import static com.dbn.credentials.SecretType.CONNECTION_HASHICORP_GITHUB_TOKEN;
import static com.dbn.credentials.SecretType.CONNECTION_HASHICORP_VAULT_PASSWORD;
import static com.dbn.credentials.SecretType.CONNECTION_HASHICORP_VAULT_TOKEN;

@Getter
@Setter
public class ConfigProviderInfo extends BasicConfiguration<ConnectionDatabaseSettings, ConfigurationEditorForm> implements Cloneable<ConfigProviderInfo>, SecretsOwner, PersistentConfiguration {
    private CloudConfigProviderType providerType;
    private CloudAuthenticationType providerAuthentication;

    private ConfigSourceType providerSourceType = ConfigSourceType.FILE;
    private String providerLocation;
    private String providerProfileKey;

    private String ociConfigFile;
    private String ociConfigProfile;

    private String awsRegion;

    private String azureAppConfigLabel;
    private String azureClientId;
    private String azureTenantId;
    private String azureClientCertificatePath;

    private String hashicorpVaultAddress;
    private String hashicorpVaultNamespace;
    private String hashicorpVaultUsername;
    private String hashicorpUserpassAuthPath;
    private String hashicorpAppRoleRoleId;
    private String hashicorpAppRoleAuthPath;
    private String hashicorpGithubAuthPath;

    private final Secret azureClientSecret = new Secret(
            CONNECTION_AZURE_CONFIG_PROVIDER_CLIENT_SECRET,
            () -> getSecretOwnerId(),
            () -> null);

    private final Secret azureClientCertificatePassword = new Secret(
            CONNECTION_AZURE_CONFIG_PROVIDER_CERTIFICATE_PASSWORD,
            () -> getSecretOwnerId(),
            () -> null);

    private final Secret hashicorpVaultToken = new Secret(
            CONNECTION_HASHICORP_VAULT_TOKEN,
            () -> getSecretOwnerId(),
            () -> null);

    private final Secret hashicorpVaultPassword = new Secret(
            CONNECTION_HASHICORP_VAULT_PASSWORD,
            () -> getSecretOwnerId(),
            () -> null);

    private final Secret hashicorpAppRoleSecretId = new Secret(
            CONNECTION_HASHICORP_APPROLE_SECRET_ID,
            () -> getSecretOwnerId(),
            () -> null);

    private final Secret hashicorpGithubToken = new Secret(
            CONNECTION_HASHICORP_GITHUB_TOKEN,
            () -> getSecretOwnerId(),
            () -> null);

    public ConfigProviderInfo(ConnectionDatabaseSettings parent) {
        super(parent);
    }

    public void reset() {
        providerSourceType = ConfigSourceType.FILE;
        providerType = null;
        providerAuthentication = null;
        ociConfigFile = null;
        ociConfigProfile = null;
        awsRegion = null;
        providerLocation = null;
        providerProfileKey = null;
        azureAppConfigLabel = null;
        azureClientId = null;
        azureTenantId = null;
        azureClientCertificatePath = null;
        hashicorpVaultAddress = null;
        hashicorpVaultNamespace = null;
        hashicorpVaultUsername = null;
        hashicorpUserpassAuthPath = null;
        hashicorpAppRoleRoleId = null;
        hashicorpAppRoleAuthPath = null;
        hashicorpGithubAuthPath = null;
    }

    @Override
    public @NotNull Object getSecretOwnerId() {
        return getConnectionSettings().getSecretOwnerId();
    }

    private @NotNull ConnectionSettings getConnectionSettings() {
        return ensureParent().ensureParent();
    }

    @Override
    public @NotNull String getSecretOwnerName() {
        return getSecretOwnerId().toString();
    }

    @Override
    public @NotNull Secret[] getSecrets() {
        return new Secret[] {
                azureClientSecret,
                azureClientCertificatePassword,
                hashicorpVaultToken,
                hashicorpVaultPassword,
                hashicorpAppRoleSecretId,
                hashicorpGithubToken};
    }

    public char[] getAzureClientSecret() {
        return azureClientSecret.getToken();
    }

    public void setAzureClientSecret(char[] token) {
        azureClientSecret.setToken(token);
    }

    public char[] getAzureClientCertificatePassword() {
        return azureClientCertificatePassword.getToken();
    }

    public void setAzureClientCertificatePassword(char[] token) {
        azureClientCertificatePassword.setToken(token);
    }

    public char[] getHashicorpVaultToken() {
        return hashicorpVaultToken.getToken();
    }

    public void setHashicorpVaultToken(char[] token) {
        hashicorpVaultToken.setToken(token);
    }

    public char[] getHashicorpVaultPassword() {
        return hashicorpVaultPassword.getToken();
    }

    public void setHashicorpVaultPassword(char[] token) {
        hashicorpVaultPassword.setToken(token);
    }

    public char[] getHashicorpAppRoleSecretId() {
        return hashicorpAppRoleSecretId.getToken();
    }

    public void setHashicorpAppRoleSecretId(char[] token) {
        hashicorpAppRoleSecretId.setToken(token);
    }

    public char[] getHashicorpGithubToken() {
        return hashicorpGithubToken.getToken();
    }

    public void setHashicorpGithubToken(char[] token) {
        hashicorpGithubToken.setToken(token);
    }

    public void applyOciAuthentication(
            CloudAuthenticationType authentication,
            String ociConfigFile,
            String ociProfile) {
        this.providerAuthentication = authentication;
        this.ociConfigFile = ociConfigFile;
        this.ociConfigProfile = ociProfile;
    }

    public void applyAzureAuthentication(
            CloudAuthenticationType authentication,
            String azureClientId,
            String azureTenantId,
            String azureClientCertificatePath) {
        this.providerAuthentication = authentication;
        this.azureClientId = isAzureProvider() && isAzureClientIdAuthentication(authentication) ? azureClientId : null;
        this.azureTenantId = isAzureProvider() && isAzureServicePrincipalAuthentication(authentication) ? azureTenantId : null;
        this.azureClientCertificatePath = isAzureProvider() && authentication == CloudAuthenticationType.AZURE_SERVICE_PRINCIPAL_CERTIFICATE ?
                azureClientCertificatePath : null;
    }

    public void applyHashicorpAuthentication(
            CloudAuthenticationType authentication,
            String vaultAddress,
            String vaultNamespace,
            String vaultUsername,
            String userPassAuthPath,
            String roleId,
            String appRoleAuthPath,
            String githubAuthPath) {
        if (isHashicorpProvider()) {
            this.providerAuthentication = authentication;
            this.hashicorpVaultAddress = vaultAddress;
            this.hashicorpVaultNamespace = vaultNamespace;
            this.hashicorpVaultUsername = authentication == CloudAuthenticationType.HCP_USERPASS ? vaultUsername : null;
            this.hashicorpUserpassAuthPath = authentication == CloudAuthenticationType.HCP_USERPASS ? userPassAuthPath : null;
            this.hashicorpAppRoleRoleId = authentication == CloudAuthenticationType.HCP_APPROLE ? roleId : null;
            this.hashicorpAppRoleAuthPath = authentication == CloudAuthenticationType.HCP_APPROLE ? appRoleAuthPath : null;
            this.hashicorpGithubAuthPath = authentication == CloudAuthenticationType.HCP_GITHUB ? githubAuthPath : null;
        } else {
            this.hashicorpVaultAddress = null;
            this.hashicorpVaultNamespace = null;
            this.hashicorpVaultUsername = null;
            this.hashicorpUserpassAuthPath = null;
            this.hashicorpAppRoleRoleId = null;
            this.hashicorpAppRoleAuthPath = null;
            this.hashicorpGithubAuthPath = null;
        }
    }

    public void apply(
            ConfigSourceType sourceType,
            CloudConfigProviderType cloudProviderType,
            String awsRegion,
            String configLocation,
            String profileKey,
            String azureAppConfigLabel) {
        this.providerSourceType = Commons.nvl(sourceType, ConfigSourceType.FILE);
        this.providerType = this.providerSourceType == ConfigSourceType.CLOUD ? cloudProviderType : null;
        this.awsRegion = isRegionConfig() ? awsRegion : null;
        this.providerProfileKey = normalizeProfileKey(profileKey);
        this.azureAppConfigLabel = isAzureAppConfig() ? azureAppConfigLabel : null;
        setProviderLocation(configLocation);
    }

    private String normalizeProfileKey(String profileKey) {
        if (!isAzureAppConfig() || isEmptyOrSpaces(profileKey)) return profileKey;

        String normalized = profileKey.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        return normalized;
    }

    public void setProviderLocation(@NonNls String providerLocation) {
        this.providerLocation = isConfigHttps() || isOciObjectStorageConfig() ?
                DatabaseUrlPattern.normalizeConfigHttpsLocation(providerLocation) :
                providerLocation;
    }

    public boolean isConfigHttps() {
        return providerSourceType == ConfigSourceType.URL;
    }

    public boolean isCloudProviderConfig() {
        return providerSourceType == ConfigSourceType.CLOUD;
    }

    public boolean isOciObjectStorageConfig() {
        return isCloudProviderConfig() && providerType == CloudConfigProviderType.OCI_OBJECT;
    }

    public boolean isRegionConfig() {
        return isCloudProviderConfig() &&
                providerType != null &&
                providerType.getRegionParameterName() != null;
    }

    public boolean isInteractiveAuthentication() {
        return isCloudProviderConfig() &&
                providerType != null &&
                ((providerType.isOci() && providerAuthentication == OCI_INTERACTIVE) ||
                 (providerType.isAzure() && providerAuthentication == AZURE_INTERACTIVE));
    }

    public boolean isAzureAppConfig() {
        return isCloudProviderConfig() && providerType == AZURE_APP_CONFIG;
    }

    private boolean isAzureProvider() {
        return isCloudProviderConfig() && providerType != null && providerType.isAzure();
    }

    private boolean isHashicorpProvider() {
        return isCloudProviderConfig() && providerType != null && providerType.isHashicorp();
    }

    public String getProviderSlug() {
        ConfigSourceType sourceType = Commons.nvl(this.providerSourceType, ConfigSourceType.FILE);
        return switch (sourceType) {
            case FILE -> "file";
            case URL -> "https";
            case CLOUD -> providerType == null ? "" : providerType.getSlug();
        };
    }

    public Map<String, String> getUrlParameters(boolean includeAuthentication) {
        @NonNls
        Map<String, String> parameters = new LinkedHashMap<>();
        if (isNotEmpty(providerProfileKey)) {
            parameters.put("key", providerProfileKey);
        }
        if (isAzureAppConfig() && isNotEmpty(azureAppConfigLabel)) {
            parameters.put("label", azureAppConfigLabel);
        }

        if (isRegionConfig() && isNotEmptyOrSpaces(awsRegion)) {
            parameters.put(providerType.getRegionParameterName(), awsRegion.trim());
        }

        if (includeAuthentication && isCloudProviderConfig() && providerType != null && providerType.isOci()) {
            parameters.putAll(OciConfigProviderParameters.build(providerAuthentication, ociConfigFile, ociConfigProfile));
        }
        if (includeAuthentication && isCloudProviderConfig() && providerType != null && providerType.isAzure() && providerAuthentication != null) {
            parameters.put("AUTHENTICATION", providerAuthentication.getParameterValue());
            if (isAzureClientIdAuthentication(providerAuthentication)) {
                if (isNotEmptyOrSpaces(azureClientId)) {
                    parameters.put("AZURE_CLIENT_ID", azureClientId.trim());
                }
            }
            if (isAzureServicePrincipalAuthentication(providerAuthentication)) {
                if (isNotEmptyOrSpaces(azureTenantId)) {
                    parameters.put("AZURE_TENANT_ID", azureTenantId.trim());
                }
                if (providerAuthentication == CloudAuthenticationType.AZURE_SERVICE_PRINCIPAL_CERTIFICATE &&
                        isNotEmptyOrSpaces(azureClientCertificatePath)) {
                    parameters.put("AZURE_CLIENT_CERTIFICATE_PATH", azureClientCertificatePath.trim());
                }
            }
        }
        if (includeAuthentication &&
                isHashicorpProvider()) {
            if (providerAuthentication != null && providerAuthentication != CloudAuthenticationType.HCP_DEFAULT) {
                parameters.put("AUTHENTICATION", providerAuthentication.getParameterValue().toUpperCase());
            }
            if (isNotEmptyOrSpaces(hashicorpVaultAddress)) {
                parameters.put("VAULT_ADDR", hashicorpVaultAddress.trim());
            }
            if (isNotEmptyOrSpaces(hashicorpVaultNamespace)) {
                parameters.put("VAULT_NAMESPACE", hashicorpVaultNamespace.trim());
            }
            if (providerAuthentication == CloudAuthenticationType.HCP_USERPASS) {
                if (isNotEmptyOrSpaces(hashicorpVaultUsername)) {
                    parameters.put("VAULT_USERNAME", hashicorpVaultUsername.trim());
                }
                if (isNotEmptyOrSpaces(hashicorpUserpassAuthPath)) {
                    parameters.put("USERPASS_AUTH_PATH", hashicorpUserpassAuthPath.trim());
                }
            }
            if (providerAuthentication == CloudAuthenticationType.HCP_APPROLE &&
                    isNotEmptyOrSpaces(hashicorpAppRoleRoleId)) {
                parameters.put("ROLE_ID", hashicorpAppRoleRoleId.trim());
            }
            if (providerAuthentication == CloudAuthenticationType.HCP_APPROLE &&
                    isNotEmptyOrSpaces(hashicorpAppRoleAuthPath)) {
                parameters.put("APPROLE_AUTH_PATH", hashicorpAppRoleAuthPath.trim());
            }
            if (providerAuthentication == CloudAuthenticationType.HCP_GITHUB &&
                    isNotEmptyOrSpaces(hashicorpGithubAuthPath)) {
                parameters.put("GITHUB_AUTH_PATH", hashicorpGithubAuthPath.trim());
            }
        }

        return parameters.isEmpty() ? Collections.emptyMap() : parameters;
    }

    public String getGcpStorageProject() {
        return getNamedLocationValue("project");
    }

    public String getGcpStorageBucket() {
        return getNamedLocationValue("bucket");
    }

    public String getGcpStorageObject() {
        return getNamedLocationValue("object");
    }

    public void applyGcpStorageLocation(String project, String bucket, String object) {
        if (isEmptyOrSpaces(project) &&
                isEmptyOrSpaces(bucket) &&
                isEmptyOrSpaces(object)) {
        setProviderLocation("");
            return;
        }

        setProviderLocation("project=" + nvl(project, "").trim() +
                ";bucket=" + nvl(bucket, "").trim() +
                ";object=" + nvl(object, "").trim());
    }

    private String getNamedLocationValue(@NonNls String key) {
        return parseNamedLocation().get(key);
    }

    public void validate(List<String> errors) {
        if (isAzureProvider() &&
                providerAuthentication == AZURE_INTERACTIVE &&
                isEmptyOrSpaces(azureClientId)) {
            errors.add("Azure interactive authentication requires client ID");
        }
        if (isAzureProvider() && isAzureServicePrincipalAuthentication(providerAuthentication)) {
            if (isEmptyOrSpaces(azureClientId)) {
                errors.add("Azure service principal authentication requires client ID");
            }
            if (isEmptyOrSpaces(azureTenantId)) {
                errors.add("Azure service principal authentication requires tenant ID");
            }
            if (providerAuthentication == CloudAuthenticationType.AZURE_SERVICE_PRINCIPAL_CERTIFICATE &&
                    isEmptyOrSpaces(azureClientCertificatePath)) {
                errors.add("Azure service principal certificate authentication requires certificate path");
            }
        }

        if (providerType != CloudConfigProviderType.GCP_STORAGE) return;

        Map<String, String> values = parseNamedLocation();
        if (isEmptyOrSpaces(values.get("project")) ||
                isEmptyOrSpaces(values.get("bucket")) ||
                isEmptyOrSpaces(values.get("object"))) {
            errors.add("GCP Cloud Storage config location requires project, bucket and object");
        }
    }

    @NonNls
    private Map<String, String> parseNamedLocation() {
        Map<String, String> values = new HashMap<>();
        if (isEmptyOrSpaces(providerLocation)) return values;

        String[] tokens = providerLocation.split(";");
        for (String token : tokens) {
            String[] entry = token.split("=", 2);
            if (entry.length != 2) continue;
            values.put(entry[0].trim().toLowerCase(), entry[1].trim());
        }
        return values;
    }

    public void initialize(String url, DatabaseUrlPattern pattern, Map<String, String> parameters) {
        reset();
        providerLocation = pattern.resolveConfigLocation(url);
        providerProfileKey = getParameterIgnoreCase(parameters, "key");
        azureAppConfigLabel = getParameterIgnoreCase(parameters, "label");

        if (pattern.getUrlType() != PROVIDER) return;

        String provider = pattern.resolveConfigProvider(url);
        if (Strings.isEmptyOrSpaces(provider)) return;

        if ("file".equalsIgnoreCase(provider)) {
            apply(ConfigSourceType.FILE, null, null, providerLocation, providerProfileKey, null);
        } else if ("https".equalsIgnoreCase(provider)) {
            apply(ConfigSourceType.URL, null, null, providerLocation, providerProfileKey, null);
        } else {
            apply(ConfigSourceType.CLOUD, CloudConfigProviderType.fromSlug(provider), null, providerLocation, providerProfileKey, azureAppConfigLabel);
        }

        if (providerType != null && providerType.isOci()) {
            providerAuthentication = CloudAuthenticationType.get(getParameterIgnoreCase(parameters, "AUTHENTICATION"));
            ociConfigFile = getParameterIgnoreCase(parameters, "OCI_CONFIG_FILE");
            ociConfigProfile = getParameterIgnoreCase(parameters, "OCI_PROFILE");
        }
        if (isAzureProvider()) {
            providerAuthentication = CloudAuthenticationType.getAzure(
                    getParameterIgnoreCase(parameters, "AUTHENTICATION"),
                    isNotEmptyOrSpaces(getParameterIgnoreCase(parameters, "AZURE_CLIENT_CERTIFICATE_PATH")));
            azureClientId = getParameterIgnoreCase(parameters, "AZURE_CLIENT_ID");
            azureTenantId = getParameterIgnoreCase(parameters, "AZURE_TENANT_ID");
            azureClientCertificatePath = getParameterIgnoreCase(parameters, "AZURE_CLIENT_CERTIFICATE_PATH");
        }
        if (isHashicorpProvider()) {
            providerAuthentication = nvl(
                    CloudAuthenticationType.get(getParameterIgnoreCase(parameters, "authentication")),
                    CloudAuthenticationType.HCP_DEFAULT);
            hashicorpVaultAddress = getParameterIgnoreCase(parameters, "VAULT_ADDR");
            hashicorpVaultNamespace = getParameterIgnoreCase(parameters, "VAULT_NAMESPACE");
            hashicorpVaultUsername = getParameterIgnoreCase(parameters, "VAULT_USERNAME");
            hashicorpUserpassAuthPath = getParameterIgnoreCase(parameters, "USERPASS_AUTH_PATH");
            hashicorpAppRoleRoleId = getParameterIgnoreCase(parameters, "ROLE_ID");
            hashicorpAppRoleAuthPath = getParameterIgnoreCase(parameters, "APPROLE_AUTH_PATH");
            hashicorpGithubAuthPath = getParameterIgnoreCase(parameters, "GITHUB_AUTH_PATH");
        }
        if (isRegionConfig()) {
            awsRegion = getParameterIgnoreCase(parameters, providerType.getRegionParameterName());
        }
    }

    private static String getParameterIgnoreCase(Map<String, String> parameters, @NonNls String key) {
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public void readConfiguration(Element element) {
        ConfigSourceType sourceType = getEnum(element, "config-file-source-type", ConfigSourceType.FILE);
        CloudConfigProviderType cloudProviderType = getEnum(element, "cloud-config-provider-type", CloudConfigProviderType.class);
        String awsRegion = getString(element, "cloud-config-provider-region", null);
        String configLocation = getString(element, "config-location", getString(element, "config-file-path", null));
        String profileKey = getString(element, "config-file-profile-key", null);
        String azureAppConfigLabel = getString(element, "cloud-config-provider-label", null);
        apply(sourceType, cloudProviderType, awsRegion, configLocation, profileKey, azureAppConfigLabel);

        CloudAuthenticationType authentication =
                getEnum(element, "cloud-config-provider-authentication", CloudAuthenticationType.class);
        if (authentication == null) {
            authentication = getEnum(element, "oci-config-provider-authentication", CloudAuthenticationType.class);
        }

        applyOciAuthentication(
                authentication,
                getString(element, "oci-config-provider-config-file", null),
                getString(element, "oci-config-provider-profile", null));
        applyAzureAuthentication(
                authentication,
                getString(element, "azure-config-provider-client-id", null),
                getString(element, "azure-config-provider-tenant-id", null),
                getString(element, "azure-config-provider-client-certificate-path", null));
        applyHashicorpAuthentication(
                authentication,
                getString(element, "hashicorp-config-provider-vault-address", null),
                getString(element, "hashicorp-config-provider-vault-namespace", null),
                getString(element, "hashicorp-config-provider-vault-username", null),
                getString(element, "hashicorp-config-provider-userpass-auth-path", null),
                getString(element, "hashicorp-config-provider-role-id", null),
                getString(element, "hashicorp-config-provider-approle-auth-path", null),
                getString(element, "hashicorp-config-provider-github-auth-path", null));
    }

    public void writeConfiguration(Element element) {
        setEnum(element, "config-file-source-type", providerSourceType);
        setEnum(element, "cloud-config-provider-type", providerType);
        setEnum(element, "cloud-config-provider-authentication", providerAuthentication);
        setString(element, "oci-config-provider-config-file", ociConfigFile);
        setString(element, "oci-config-provider-profile", ociConfigProfile);
        setString(element, "azure-config-provider-client-id", azureClientId);
        setString(element, "azure-config-provider-tenant-id", azureTenantId);
        setString(element, "azure-config-provider-client-certificate-path", azureClientCertificatePath);
        setString(element, "hashicorp-config-provider-vault-address", hashicorpVaultAddress);
        setString(element, "hashicorp-config-provider-vault-namespace", hashicorpVaultNamespace);
        setString(element, "hashicorp-config-provider-vault-username", hashicorpVaultUsername);
        setString(element, "hashicorp-config-provider-userpass-auth-path", hashicorpUserpassAuthPath);
        setString(element, "hashicorp-config-provider-role-id", hashicorpAppRoleRoleId);
        setString(element, "hashicorp-config-provider-approle-auth-path", hashicorpAppRoleAuthPath);
        setString(element, "hashicorp-config-provider-github-auth-path", hashicorpGithubAuthPath);
        setString(element, "cloud-config-provider-region", awsRegion);
        setString(element, "config-location", providerLocation);
        setString(element, "config-file-profile-key", providerProfileKey);
        setString(element, "cloud-config-provider-label", azureAppConfigLabel);
    }

    @Override
    public ConfigProviderInfo clone() {
        ConfigProviderInfo clone = new ConfigProviderInfo(getParent());
        clone.providerSourceType = providerSourceType;
        clone.providerType = providerType;
        clone.providerAuthentication = providerAuthentication;
        clone.ociConfigFile = ociConfigFile;
        clone.ociConfigProfile = ociConfigProfile;
        clone.awsRegion = awsRegion;
        clone.providerLocation = providerLocation;
        clone.providerProfileKey = providerProfileKey;
        clone.azureAppConfigLabel = azureAppConfigLabel;
        clone.azureClientId = azureClientId;
        clone.azureTenantId = azureTenantId;
        clone.azureClientCertificatePath = azureClientCertificatePath;
        clone.hashicorpVaultAddress = hashicorpVaultAddress;
        clone.hashicorpVaultNamespace = hashicorpVaultNamespace;
        clone.hashicorpVaultUsername = hashicorpVaultUsername;
        clone.hashicorpUserpassAuthPath = hashicorpUserpassAuthPath;
        clone.hashicorpAppRoleRoleId = hashicorpAppRoleRoleId;
        clone.hashicorpAppRoleAuthPath = hashicorpAppRoleAuthPath;
        clone.hashicorpGithubAuthPath = hashicorpGithubAuthPath;
        clone.azureClientSecret.setToken(azureClientSecret);
        clone.azureClientCertificatePassword.setToken(azureClientCertificatePassword);
        clone.hashicorpVaultToken.setToken(hashicorpVaultToken);
        clone.hashicorpVaultPassword.setToken(hashicorpVaultPassword);
        clone.hashicorpAppRoleSecretId.setToken(hashicorpAppRoleSecretId);
        clone.hashicorpGithubToken.setToken(hashicorpGithubToken);
        return clone;
    }

    private static boolean isAzureClientIdAuthentication(CloudAuthenticationType authentication) {
        return authentication == AZURE_INTERACTIVE ||
                isAzureServicePrincipalAuthentication(authentication);
    }

    private static boolean isAzureServicePrincipalAuthentication(CloudAuthenticationType authentication) {
        return authentication == CloudAuthenticationType.AZURE_SERVICE_PRINCIPAL_SECRET ||
                authentication == CloudAuthenticationType.AZURE_SERVICE_PRINCIPAL_CERTIFICATE;
    }
}
