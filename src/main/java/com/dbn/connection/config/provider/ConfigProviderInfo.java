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
import com.dbn.connection.config.provider.impl.CloudConfigProviderHandler;
import com.dbn.connection.config.provider.impl.CloudConfigProviderHandlers;
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

import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.getEnum;
import static com.dbn.common.options.setting.Settings.getString;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setEnum;
import static com.dbn.common.options.setting.Settings.setString;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Strings.isEmptyOrSpaces;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.connection.DatabaseUrlType.PROVIDER;
import static com.dbn.connection.config.provider.CloudAuthenticationType.AZURE_INTERACTIVE;
import static com.dbn.connection.config.provider.CloudAuthenticationType.OCI_INTERACTIVE;
import static com.dbn.connection.config.provider.CloudConfigProviderType.AZURE_APP_CONFIG;
import static com.dbn.connection.config.provider.CloudConfigProviderType.GCP_STORAGE;
import static com.dbn.connection.config.provider.impl.GcpConfigProviderHandler.applyStorageLocation;
import static com.dbn.connection.config.provider.impl.GcpConfigProviderHandler.getStorageLocation;
import static com.dbn.credentials.SecretType.CONNECTION_AZURE_CONFIG_PROVIDER_CERTIFICATE_PASSWORD;
import static com.dbn.credentials.SecretType.CONNECTION_AZURE_CONFIG_PROVIDER_CLIENT_SECRET;
import static com.dbn.credentials.SecretType.CONNECTION_HASHICORP_APPROLE_SECRET_ID;
import static com.dbn.credentials.SecretType.CONNECTION_HASHICORP_GITHUB_TOKEN;
import static com.dbn.credentials.SecretType.CONNECTION_HASHICORP_VAULT_PASSWORD;
import static com.dbn.credentials.SecretType.CONNECTION_HASHICORP_VAULT_TOKEN;

@Getter
@Setter
public class ConfigProviderInfo extends BasicConfiguration<ConnectionDatabaseSettings, ConfigurationEditorForm> implements Cloneable<ConfigProviderInfo>, SecretsOwner, PersistentConfiguration {
    private ConfigSourceType providerSourceType = ConfigSourceType.FILE;
    private CloudConfigProviderType cloudProviderType;
    private CloudAuthenticationType cloudAuthenticationType;

    private Map<String, String> providerLocations = new HashMap<>();
    private String providerProfileKey;

    private String ociConfigFile;
    private String ociConfigProfile;

    private String awsRegion;

    private String gcpStorageProject;
    private String gcpStorageBucket;
    private String gcpStorageObject;

    private String azureAppConfigLabel;
    private String azureClientId;
    private String azureTenantId;
    private String azureClientCertificatePath;

    private String hashicorpVaultAddress;
    private String hashicorpVaultNamespace;
    private String hashicorpVaultUsername;
    private String hashicorpUserpassAuthPath;
    private String hashicorpAppRoleId;
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
        cloudProviderType = null;
        cloudAuthenticationType = null;
        ociConfigFile = null;
        ociConfigProfile = null;
        awsRegion = null;
        gcpStorageProject = null;
        gcpStorageBucket = null;
        gcpStorageObject = null;
        providerLocations.clear();
        providerProfileKey = null;
        azureAppConfigLabel = null;
        azureClientId = null;
        azureTenantId = null;
        azureClientCertificatePath = null;
        hashicorpVaultAddress = null;
        hashicorpVaultNamespace = null;
        hashicorpVaultUsername = null;
        hashicorpUserpassAuthPath = null;
        hashicorpAppRoleId = null;
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

    public void setProviderProfileKey(String profileKey) {
        this.providerProfileKey = normalizeProfileKey(profileKey);
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
        if (isGcpStorageConfig()) {
            applyStorageLocation(this, providerLocation);
            return;
        }

        String normalizedLocation = isConfigHttps() || isOciObjectStorageConfig() ?
                DatabaseUrlPattern.normalizeConfigHttpsLocation(providerLocation) :
                providerLocation;
        providerLocations.put(getProviderLocationKey(), normalizedLocation);
    }

    public String getProviderLocation() {
        return getProviderLocation(providerSourceType, cloudProviderType);
    }

    public String getProviderLocation(ConfigSourceType sourceType, CloudConfigProviderType providerType) {
        if (sourceType == ConfigSourceType.CLOUD && providerType == GCP_STORAGE) {
            return getStorageLocation(gcpStorageProject, gcpStorageBucket, gcpStorageObject);
        }

        return providerLocations.get(getProviderLocationKey(sourceType, providerType));
    }

    private static String getProviderLocationKey(ConfigSourceType providerSourceType, CloudConfigProviderType cloudProviderType) {
        if (providerSourceType == ConfigSourceType.CLOUD) return providerSourceType + ":" + cloudProviderType;
        return providerSourceType.name();
    }

    private String getProviderLocationKey() {
        return getProviderLocationKey(providerSourceType, cloudProviderType);
    }

    public boolean isConfigHttps() {
        return providerSourceType == ConfigSourceType.URL;
    }

    public boolean isCloudProviderConfig() {
        return providerSourceType == ConfigSourceType.CLOUD;
    }

    public boolean isOciObjectStorageConfig() {
        return isCloudProviderConfig() && cloudProviderType == CloudConfigProviderType.OCI_OBJECT;
    }

    public boolean isGcpStorageConfig() {
        return isCloudProviderConfig() && cloudProviderType == GCP_STORAGE;
    }

    public boolean isAwsRegionConfig() {
        return isCloudProviderConfig() &&
                cloudProviderType != null &&
                cloudProviderType.getAwsRegionParameterName() != null;
    }

    public boolean isInteractiveAuthentication() {
        return isCloudProviderConfig() &&
                cloudProviderType != null &&
                ((cloudProviderType.isOci() && cloudAuthenticationType == OCI_INTERACTIVE) ||
                 (cloudProviderType.isAzure() && cloudAuthenticationType == AZURE_INTERACTIVE));
    }

    public boolean isAzureAppConfig() {
        return isCloudProviderConfig() && cloudProviderType == AZURE_APP_CONFIG;
    }


    @NotNull
    public CloudConfigProviderHandler getHandler() {
        return CloudConfigProviderHandlers.get(cloudProviderType);
    }

    public String getProviderSlug() {
        ConfigSourceType sourceType = Commons.nvl(this.providerSourceType, ConfigSourceType.FILE);
        return switch (sourceType) {
            case FILE -> "file";
            case URL -> "https";
            case CLOUD -> cloudProviderType == null ? "" : cloudProviderType.getSlug();
        };
    }

    public Map<String, String> getUrlParameters(boolean includeAuthentication) {
        @NonNls
        Map<String, String> parameters = new LinkedHashMap<>();
        if (isNotEmpty(providerProfileKey)) {
            parameters.put("key", providerProfileKey);
        }
        CloudConfigProviderHandler handler = getHandler();
        handler.addUrlParameters(parameters, this, includeAuthentication);

        return parameters.isEmpty() ? Collections.emptyMap() : parameters;
    }

    public void validate(List<String> errors) {
        getHandler().validate(this, errors);
    }

    public void initialize(String url, DatabaseUrlPattern pattern, Map<String, String> parameters) {
        reset();
        providerLocations.clear();
        if (pattern.getUrlType() != PROVIDER) return;

        String provider = pattern.resolveConfigProvider(url);
        if (Strings.isEmptyOrSpaces(provider)) return;

        if ("file".equalsIgnoreCase(provider)) {
            this.providerSourceType = ConfigSourceType.FILE;
            this.cloudProviderType = null;
        } else if ("https".equalsIgnoreCase(provider)) {
            this.providerSourceType = ConfigSourceType.URL;
            this.cloudProviderType = null;
        } else {
            this.providerSourceType = ConfigSourceType.CLOUD;
            this.cloudProviderType = CloudConfigProviderType.fromSlug(provider);
        }

        String providerLocation = pattern.resolveConfigLocation(url);
        setProviderLocation(providerLocation);
        getHandler().initialize(this, parameters);
    }

    public void readConfiguration(Element element) {
        if (element == null) return;

        providerSourceType = getEnum(element, "provider-source-type", ConfigSourceType.FILE);
        cloudAuthenticationType = getEnum(element, "cloud-provider-authentication", CloudAuthenticationType.class);
        cloudProviderType = getEnum(element, "cloud-provider-type", CloudConfigProviderType.class);

        providerLocations.clear();
        Element locationsElement = element.getChild("provider-locations");
        for (Element locationElement : childrenOf(locationsElement, "location")) {
            ConfigSourceType sourceType = enumAttribute(locationElement, "source-type", ConfigSourceType.class);
            CloudConfigProviderType providerType = enumAttribute(locationElement, "provider-type", CloudConfigProviderType.class);
            String location = stringAttribute(locationElement, "value");
            if (sourceType != null && location != null &&
                    !(sourceType == ConfigSourceType.CLOUD && providerType == GCP_STORAGE)) {
                String locationKey = getProviderLocationKey(sourceType, providerType);
                providerLocations.put(locationKey, location);
            }
        }
        providerProfileKey = getString(element, "provider-profile-key", null);

        ociConfigFile = getString(element, "oci-config-file", null);
        ociConfigProfile = getString(element, "oci-config-profile", null);

        azureClientId = getString(element, "azure-client-id", null);
        azureTenantId = getString(element, "azure-tenant-id", null);
        azureAppConfigLabel = getString(element, "azure-app-config-label", null);
        azureClientCertificatePath = getString(element, "azure-client-certificate-path", null);

        hashicorpVaultAddress = getString(element, "hashicorp-vault-address", null);
        hashicorpVaultNamespace = getString(element, "hashicorp-vault-namespace", null);
        hashicorpVaultUsername = getString(element, "hashicorp-vault-username", null);
        hashicorpUserpassAuthPath = getString(element, "hashicorp-userpass-auth-path", null);
        hashicorpAppRoleId = getString(element, "hashicorp-approle-role-id", null);
        hashicorpAppRoleAuthPath = getString(element, "hashicorp-approle-auth-path", null);
        hashicorpGithubAuthPath = getString(element, "hashicorp-github-auth-path", null);

        awsRegion = getString(element, "aws-region", null);
        gcpStorageProject = getString(element, "gcp-storage-project", null);
        gcpStorageBucket = getString(element, "gcp-storage-bucket", null);
        gcpStorageObject = getString(element, "gcp-storage-object", null);

    }

    public void writeConfiguration(Element element) {
        setEnum(element, "provider-source-type", providerSourceType);
        setEnum(element, "cloud-provider-type", cloudProviderType);
        setEnum(element, "cloud-provider-authentication", cloudAuthenticationType);

        setString(element, "oci-config-file", ociConfigFile);
        setString(element, "oci-config-profile", ociConfigProfile);

        setString(element, "azure-client-id", azureClientId);
        setString(element, "azure-tenant-id", azureTenantId);
        setString(element, "azure-client-certificate-path", azureClientCertificatePath);
        setString(element, "azure-app-config-label", azureAppConfigLabel);

        setString(element, "hashicorp-vault-address", hashicorpVaultAddress);
        setString(element, "hashicorp-vault-namespace", hashicorpVaultNamespace);
        setString(element, "hashicorp-vault-username", hashicorpVaultUsername);
        setString(element, "hashicorp-userpass-auth-path", hashicorpUserpassAuthPath);
        setString(element, "hashicorp-approle-role-id", hashicorpAppRoleId);
        setString(element, "hashicorp-approle-auth-path", hashicorpAppRoleAuthPath);
        setString(element, "hashicorp-github-auth-path", hashicorpGithubAuthPath);

        setString(element, "aws-region", awsRegion);
        setString(element, "gcp-storage-project", gcpStorageProject);
        setString(element, "gcp-storage-bucket", gcpStorageBucket);
        setString(element, "gcp-storage-object", gcpStorageObject);

        Element locationsElement = newElement(element, "provider-locations");
        providerLocations.forEach((key, location) -> {
            String[] keyParts = key.split(":", 2);
            if (ConfigSourceType.CLOUD.name().equals(keyParts[0]) && GCP_STORAGE.name().equals(keyParts[1])) return;

            Element locationElement = newElement(locationsElement, "location");
            setStringAttribute(locationElement, "source-type", keyParts[0]);
            if (keyParts.length > 1 && !keyParts[1].isEmpty()) {
                setStringAttribute(locationElement, "provider-type", keyParts[1]);
            }
            setStringAttribute(locationElement, "value", location);
        });
        setString(element, "provider-profile-key", providerProfileKey);
    }

    @Override
    public ConfigProviderInfo clone() {
        ConfigProviderInfo clone = new ConfigProviderInfo(getParent());
        clone.providerSourceType = this.providerSourceType;
        clone.cloudProviderType = this.cloudProviderType;
        clone.cloudAuthenticationType = this.cloudAuthenticationType;

        clone.providerLocations = new HashMap<>(providerLocations);
        clone.providerProfileKey = this.providerProfileKey;

        clone.ociConfigFile = this.ociConfigFile;
        clone.ociConfigProfile = this.ociConfigProfile;

        clone.awsRegion = this.awsRegion;

        clone.gcpStorageProject = this.gcpStorageProject;
        clone.gcpStorageBucket = this.gcpStorageBucket;
        clone.gcpStorageObject = this.gcpStorageObject;

        clone.azureAppConfigLabel = this.azureAppConfigLabel;
        clone.azureClientId = this.azureClientId;
        clone.azureTenantId = this.azureTenantId;
        clone.azureClientCertificatePath = this.azureClientCertificatePath;
        clone.azureClientSecret.setToken(this.azureClientSecret);
        clone.azureClientCertificatePassword.setToken(this.azureClientCertificatePassword);

        clone.hashicorpVaultAddress = this.hashicorpVaultAddress;
        clone.hashicorpVaultNamespace = this.hashicorpVaultNamespace;
        clone.hashicorpVaultUsername = this.hashicorpVaultUsername;
        clone.hashicorpUserpassAuthPath = this.hashicorpUserpassAuthPath;
        clone.hashicorpAppRoleId = this.hashicorpAppRoleId;
        clone.hashicorpAppRoleAuthPath = this.hashicorpAppRoleAuthPath;
        clone.hashicorpGithubAuthPath = this.hashicorpGithubAuthPath;
        clone.hashicorpVaultToken.setToken(this.hashicorpVaultToken);
        clone.hashicorpVaultPassword.setToken(this.hashicorpVaultPassword);
        clone.hashicorpAppRoleSecretId.setToken(this.hashicorpAppRoleSecretId);
        clone.hashicorpGithubToken.setToken(this.hashicorpGithubToken);
        return clone;
    }

}
