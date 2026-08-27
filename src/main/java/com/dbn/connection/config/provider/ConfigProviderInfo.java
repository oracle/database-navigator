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
import com.dbn.connection.config.provider.impl.AzureConfigProviderHandler;
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
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Strings.isEmptyOrSpaces;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.connection.DatabaseUrlType.PROVIDER;
import static com.dbn.connection.config.provider.CloudAuthenticationType.AZURE_INTERACTIVE;
import static com.dbn.connection.config.provider.CloudAuthenticationType.AZURE_SERVICE_PRINCIPAL_CERTIFICATE;
import static com.dbn.connection.config.provider.CloudAuthenticationType.HCP_DEFAULT;
import static com.dbn.connection.config.provider.CloudAuthenticationType.OCI_INTERACTIVE;
import static com.dbn.connection.config.provider.CloudAuthenticationType.get;
import static com.dbn.connection.config.provider.CloudAuthenticationType.getAzure;
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
    private ConfigSourceType providerSourceType = ConfigSourceType.FILE;
    private CloudConfigProviderType cloudProviderType;
    private CloudAuthenticationType cloudProviderAuthentication;

    private Map<String, String> providerLocations = new HashMap<>();
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
        cloudProviderAuthentication = null;
        ociConfigFile = null;
        ociConfigProfile = null;
        awsRegion = null;
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

    public void apply(
            ConfigSourceType sourceType,
            CloudConfigProviderType cloudProviderType,
            String awsRegion,
            String configLocation,
            String profileKey,
            String azureAppConfigLabel) {
        this.providerSourceType = Commons.nvl(sourceType, ConfigSourceType.FILE);
        this.cloudProviderType = this.providerSourceType == ConfigSourceType.CLOUD ? cloudProviderType : null;
        this.awsRegion = isAwsRegionConfig() ? awsRegion : null;
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
        String normalizedLocation = isConfigHttps() || isOciObjectStorageConfig() ?
                DatabaseUrlPattern.normalizeConfigHttpsLocation(providerLocation) :
                providerLocation;
        providerLocations.put(getProviderLocationKey(), normalizedLocation);
    }

    public String getProviderLocation() {
        return providerLocations.get(getProviderLocationKey());
    }

    public String getProviderLocation(ConfigSourceType sourceType, CloudConfigProviderType providerType) {
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

    public boolean isAwsRegionConfig() {
        return isCloudProviderConfig() &&
                cloudProviderType != null &&
                cloudProviderType.getAwsRegionParameterName() != null;
    }

    public boolean isInteractiveAuthentication() {
        return isCloudProviderConfig() &&
                cloudProviderType != null &&
                ((cloudProviderType.isOci() && cloudProviderAuthentication == OCI_INTERACTIVE) ||
                 (cloudProviderType.isAzure() && cloudProviderAuthentication == AZURE_INTERACTIVE));
    }

    public boolean isAzureAppConfig() {
        return isCloudProviderConfig() && cloudProviderType == AZURE_APP_CONFIG;
    }

    public boolean isOciProvider() {
        return isCloudProviderConfig() && cloudProviderType != null && cloudProviderType.isOci();
    }

    public boolean isAzureProvider() {
        return isCloudProviderConfig() && cloudProviderType != null && cloudProviderType.isAzure();
    }

    public boolean isHashicorpProvider() {
        return isCloudProviderConfig() && cloudProviderType != null && cloudProviderType.isHashicorp();
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
                cloudProviderAuthentication == AZURE_INTERACTIVE &&
                isEmptyOrSpaces(azureClientId)) {
            errors.add("Azure interactive authentication requires client ID");
        }
        if (isAzureProvider() && AzureConfigProviderHandler.isAzureServicePrincipalAuthentication(cloudProviderAuthentication)) {
            if (isEmptyOrSpaces(azureClientId)) {
                errors.add("Azure service principal authentication requires client ID");
            }
            if (isEmptyOrSpaces(azureTenantId)) {
                errors.add("Azure service principal authentication requires tenant ID");
            }
            if (cloudProviderAuthentication == AZURE_SERVICE_PRINCIPAL_CERTIFICATE && isEmptyOrSpaces(azureClientCertificatePath)) {
                errors.add("Azure service principal certificate authentication requires certificate path");
            }
        }

        if (cloudProviderType != CloudConfigProviderType.GCP_STORAGE) return;

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
        String providerLocation = getProviderLocation();
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
        providerLocations.clear();
        String providerLocation = pattern.resolveConfigLocation(url);
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

        if (isOciProvider()) {
            cloudProviderAuthentication = get(getParameterIgnoreCase(parameters, "AUTHENTICATION"));
            ociConfigFile = getParameterIgnoreCase(parameters, "OCI_CONFIG_FILE");
            ociConfigProfile = getParameterIgnoreCase(parameters, "OCI_PROFILE");

        } else if (isAzureProvider()) {
            cloudProviderAuthentication = getAzure(
                    getParameterIgnoreCase(parameters, "AUTHENTICATION"),
                    isNotEmptyOrSpaces(getParameterIgnoreCase(parameters, "AZURE_CLIENT_CERTIFICATE_PATH")));
            azureClientId = getParameterIgnoreCase(parameters, "AZURE_CLIENT_ID");
            azureTenantId = getParameterIgnoreCase(parameters, "AZURE_TENANT_ID");
            azureClientCertificatePath = getParameterIgnoreCase(parameters, "AZURE_CLIENT_CERTIFICATE_PATH");

        } else if (isHashicorpProvider()) {
            cloudProviderAuthentication = nvl(
                    get(getParameterIgnoreCase(parameters, "authentication")),
                    HCP_DEFAULT);
            hashicorpVaultAddress = getParameterIgnoreCase(parameters, "VAULT_ADDR");
            hashicorpVaultNamespace = getParameterIgnoreCase(parameters, "VAULT_NAMESPACE");
            hashicorpVaultUsername = getParameterIgnoreCase(parameters, "VAULT_USERNAME");
            hashicorpUserpassAuthPath = getParameterIgnoreCase(parameters, "USERPASS_AUTH_PATH");
            hashicorpAppRoleId = getParameterIgnoreCase(parameters, "ROLE_ID");
            hashicorpAppRoleAuthPath = getParameterIgnoreCase(parameters, "APPROLE_AUTH_PATH");
            hashicorpGithubAuthPath = getParameterIgnoreCase(parameters, "GITHUB_AUTH_PATH");
        }
        if (isAwsRegionConfig()) {
            awsRegion = getParameterIgnoreCase(parameters, cloudProviderType.getAwsRegionParameterName());
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
        if (element == null) return;

        providerSourceType = getEnum(element, "provider-source-type", ConfigSourceType.FILE);
        cloudProviderAuthentication = getEnum(element, "cloud-provider-authentication", CloudAuthenticationType.class);
        cloudProviderType = getEnum(element, "cloud-provider-type", CloudConfigProviderType.class);

        providerLocations.clear();
        Element locationsElement = element.getChild("provider-locations");
        for (Element locationElement : childrenOf(locationsElement, "location")) {
            ConfigSourceType sourceType = enumAttribute(locationElement, "source-type", ConfigSourceType.class);
            CloudConfigProviderType providerType = enumAttribute(locationElement, "provider-type", CloudConfigProviderType.class);
            String location = stringAttribute(locationElement, "value");
            if (sourceType != null && location != null) {
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

    }

    public void writeConfiguration(Element element) {
        setEnum(element, "provider-source-type", providerSourceType);
        setEnum(element, "cloud-provider-type", cloudProviderType);
        setEnum(element, "cloud-provider-authentication", cloudProviderAuthentication);

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

        Element locationsElement = newElement(element, "provider-locations");
        providerLocations.forEach((key, location) -> {
            String[] keyParts = key.split(":", 2);
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
        clone.cloudProviderAuthentication = this.cloudProviderAuthentication;

        clone.providerLocations = new HashMap<>(providerLocations);
        clone.providerProfileKey = this.providerProfileKey;

        clone.ociConfigFile = this.ociConfigFile;
        clone.ociConfigProfile = this.ociConfigProfile;

        clone.awsRegion = this.awsRegion;

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
