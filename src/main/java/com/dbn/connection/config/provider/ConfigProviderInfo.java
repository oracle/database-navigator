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

import com.dbn.common.util.Cloneable;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.DatabaseUrlPattern;
import com.dbn.connection.config.OciConfigProviderParameters;
import com.dbn.credentials.Secret;
import com.dbn.credentials.SecretsOwner;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
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
import static com.dbn.common.dispose.Failsafe.nn;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Strings.isEmptyOrSpaces;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.connection.DatabaseUrlType.CONFIG_FILE;
import static com.dbn.credentials.SecretType.CONNECTION_AZURE_CONFIG_PROVIDER_CERTIFICATE_PASSWORD;
import static com.dbn.credentials.SecretType.CONNECTION_AZURE_CONFIG_PROVIDER_CLIENT_SECRET;
import static com.dbn.credentials.SecretType.CONNECTION_HASHICORP_APPROLE_SECRET_ID;
import static com.dbn.credentials.SecretType.CONNECTION_HASHICORP_GITHUB_TOKEN;
import static com.dbn.credentials.SecretType.CONNECTION_HASHICORP_VAULT_PASSWORD;
import static com.dbn.credentials.SecretType.CONNECTION_HASHICORP_VAULT_TOKEN;

@Getter
@Setter
@EqualsAndHashCode
public class ConfigProviderInfo implements Cloneable<ConfigProviderInfo>, SecretsOwner {
    private ConfigFileSourceType sourceType = ConfigFileSourceType.LOCAL_FILE;
    private CloudConfigProviderType cloudProviderType;
    private CloudConfigProviderAuthentication authentication;
    private String ociConfigFile;
    private String ociProfile;
    private String region;
    private String location;
    private String profileKey;
    private String label;
    private String azureClientId;
    private String azureTenantId;
    private String azureClientCertificatePath;
    private String vaultAddress;
    private String vaultNamespace;
    private String vaultUsername;
    private String userPassAuthPath;
    private String roleId;
    private String appRoleAuthPath;
    private String githubAuthPath;
    private transient ConnectionId credentialConnectionId;
    @EqualsAndHashCode.Exclude
    private final Secret azureClientSecret = new Secret(
            CONNECTION_AZURE_CONFIG_PROVIDER_CLIENT_SECRET,
            this::getSecretOwnerId,
            () -> null);
    @EqualsAndHashCode.Exclude
    private final Secret azureClientCertificatePassword = new Secret(
            CONNECTION_AZURE_CONFIG_PROVIDER_CERTIFICATE_PASSWORD,
            this::getSecretOwnerId,
            () -> null);
    @EqualsAndHashCode.Exclude
    private final Secret hashicorpVaultToken = new Secret(
            CONNECTION_HASHICORP_VAULT_TOKEN,
            this::getSecretOwnerId,
            () -> null);
    @EqualsAndHashCode.Exclude
    private final Secret hashicorpVaultPassword = new Secret(
            CONNECTION_HASHICORP_VAULT_PASSWORD,
            this::getSecretOwnerId,
            () -> null);
    @EqualsAndHashCode.Exclude
    private final Secret hashicorpAppRoleSecretId = new Secret(
            CONNECTION_HASHICORP_APPROLE_SECRET_ID,
            this::getSecretOwnerId,
            () -> null);
    @EqualsAndHashCode.Exclude
    private final Secret hashicorpGithubToken = new Secret(
            CONNECTION_HASHICORP_GITHUB_TOKEN,
            this::getSecretOwnerId,
            () -> null);

    public void reset() {
        sourceType = ConfigFileSourceType.LOCAL_FILE;
        cloudProviderType = null;
        authentication = null;
        ociConfigFile = null;
        ociProfile = null;
        region = null;
        location = null;
        profileKey = null;
        label = null;
        azureClientId = null;
        azureTenantId = null;
        azureClientCertificatePath = null;
        vaultAddress = null;
        vaultNamespace = null;
        vaultUsername = null;
        userPassAuthPath = null;
        roleId = null;
        appRoleAuthPath = null;
        githubAuthPath = null;
    }

    @Override
    public @NotNull Object getSecretOwnerId() {
        return nn(credentialConnectionId);
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
            CloudConfigProviderAuthentication authentication,
            String ociConfigFile,
            String ociProfile) {
        this.authentication = authentication;
        this.ociConfigFile = ociConfigFile;
        this.ociProfile = ociProfile;
    }

    public void applyAzureAuthentication(
            CloudConfigProviderAuthentication authentication,
            String azureClientId,
            String azureTenantId,
            String azureClientCertificatePath) {
        this.authentication = authentication;
        this.azureClientId = isAzureProvider() && isAzureClientIdAuthentication(authentication) ? azureClientId : null;
        this.azureTenantId = isAzureProvider() && isAzureServicePrincipalAuthentication(authentication) ? azureTenantId : null;
        this.azureClientCertificatePath = isAzureProvider() && authentication == CloudConfigProviderAuthentication.AZURE_SERVICE_PRINCIPAL_CERTIFICATE ?
                azureClientCertificatePath : null;
    }

    public void applyHashicorpAuthentication(
            CloudConfigProviderAuthentication authentication,
            String vaultAddress,
            String vaultNamespace,
            String vaultUsername,
            String userPassAuthPath,
            String roleId,
            String appRoleAuthPath,
            String githubAuthPath) {
        if (isHashicorpProvider()) {
            this.authentication = authentication;
            this.vaultAddress = vaultAddress;
            this.vaultNamespace = vaultNamespace;
            this.vaultUsername = authentication == CloudConfigProviderAuthentication.HCP_USERPASS ? vaultUsername : null;
            this.userPassAuthPath = authentication == CloudConfigProviderAuthentication.HCP_USERPASS ? userPassAuthPath : null;
            this.roleId = authentication == CloudConfigProviderAuthentication.HCP_APPROLE ? roleId : null;
            this.appRoleAuthPath = authentication == CloudConfigProviderAuthentication.HCP_APPROLE ? appRoleAuthPath : null;
            this.githubAuthPath = authentication == CloudConfigProviderAuthentication.HCP_GITHUB ? githubAuthPath : null;
        } else {
            this.vaultAddress = null;
            this.vaultNamespace = null;
            this.vaultUsername = null;
            this.userPassAuthPath = null;
            this.roleId = null;
            this.appRoleAuthPath = null;
            this.githubAuthPath = null;
        }
    }

    public void apply(
            ConfigFileSourceType sourceType,
            CloudConfigProviderType cloudProviderType,
            String region,
            String location,
            String profileKey,
            String label) {
        this.sourceType = Commons.nvl(sourceType, ConfigFileSourceType.LOCAL_FILE);
        this.cloudProviderType = this.sourceType == ConfigFileSourceType.CLOUD_PROVIDER ? cloudProviderType : null;
        this.region = isRegionConfig() ? region : null;
        this.profileKey = normalizeProfileKey(profileKey);
        this.label = isAzureAppConfig() ? label : null;
        setLocation(location);
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

    public void setLocation(String location) {
        this.location = isConfigHttps() || isOciObjectStorageConfig() ?
                DatabaseUrlPattern.normalizeConfigHttpsLocation(location) :
                location;
    }

    public boolean isConfigHttps() {
        return sourceType == ConfigFileSourceType.HTTPS;
    }

    public boolean isCloudProviderConfig() {
        return sourceType == ConfigFileSourceType.CLOUD_PROVIDER;
    }

    public boolean isOciObjectStorageConfig() {
        return isCloudProviderConfig() && cloudProviderType == CloudConfigProviderType.OCI_OBJECT;
    }

    public boolean isRegionConfig() {
        return isCloudProviderConfig() &&
                cloudProviderType != null &&
                cloudProviderType.getRegionParameterName() != null;
    }

    public boolean isInteractiveAuthentication() {
        return isCloudProviderConfig() &&
                cloudProviderType != null &&
                ((cloudProviderType.isOci() &&
                        authentication == CloudConfigProviderAuthentication.OCI_INTERACTIVE) ||
                 (cloudProviderType.isAzure() &&
                        authentication == CloudConfigProviderAuthentication.AZURE_INTERACTIVE));
    }

    public boolean isAzureAppConfig() {
        return isCloudProviderConfig() && cloudProviderType == CloudConfigProviderType.AZURE_APP_CONFIG;
    }

    private boolean isAzureProvider() {
        return isCloudProviderConfig() && cloudProviderType != null && cloudProviderType.isAzure();
    }

    private boolean isHashicorpProvider() {
        return isCloudProviderConfig() && cloudProviderType != null && cloudProviderType.isHashicorp();
    }

    public String getProviderSlug() {
        ConfigFileSourceType sourceType = Commons.nvl(this.sourceType, ConfigFileSourceType.LOCAL_FILE);
        return switch (sourceType) {
            case LOCAL_FILE -> "file";
            case HTTPS -> "https";
            case CLOUD_PROVIDER -> cloudProviderType == null ? "" : cloudProviderType.getSlug();
        };
    }

    public Map<String, String> getUrlParameters(boolean includeAuthentication) {
        Map<String, String> parameters = new LinkedHashMap<>();
        if (isNotEmpty(profileKey)) {
            parameters.put("key", profileKey);
        }
        if (isAzureAppConfig() && isNotEmpty(label)) {
            parameters.put("label", label);
        }

        if (isRegionConfig() && isNotEmptyOrSpaces(region)) {
            parameters.put(cloudProviderType.getRegionParameterName(), region.trim());
        }

        if (includeAuthentication && isCloudProviderConfig() && cloudProviderType != null && cloudProviderType.isOci()) {
            parameters.putAll(OciConfigProviderParameters.build(authentication, ociConfigFile, ociProfile));
        }
        if (includeAuthentication && isCloudProviderConfig() && cloudProviderType != null && cloudProviderType.isAzure() && authentication != null) {
            parameters.put("AUTHENTICATION", authentication.getParameterValue());
            if (isAzureClientIdAuthentication(authentication)) {
                if (isNotEmptyOrSpaces(azureClientId)) {
                    parameters.put("AZURE_CLIENT_ID", azureClientId.trim());
                }
            }
            if (isAzureServicePrincipalAuthentication(authentication)) {
                if (isNotEmptyOrSpaces(azureTenantId)) {
                    parameters.put("AZURE_TENANT_ID", azureTenantId.trim());
                }
                if (authentication == CloudConfigProviderAuthentication.AZURE_SERVICE_PRINCIPAL_CERTIFICATE &&
                        isNotEmptyOrSpaces(azureClientCertificatePath)) {
                    parameters.put("AZURE_CLIENT_CERTIFICATE_PATH", azureClientCertificatePath.trim());
                }
            }
        }
        if (includeAuthentication &&
                isHashicorpProvider()) {
            if (authentication != null && authentication != CloudConfigProviderAuthentication.HCP_DEFAULT) {
                parameters.put("AUTHENTICATION", authentication.getParameterValue().toUpperCase());
            }
            if (isNotEmptyOrSpaces(vaultAddress)) {
                parameters.put("VAULT_ADDR", vaultAddress.trim());
            }
            if (isNotEmptyOrSpaces(vaultNamespace)) {
                parameters.put("VAULT_NAMESPACE", vaultNamespace.trim());
            }
            if (authentication == CloudConfigProviderAuthentication.HCP_USERPASS) {
                if (isNotEmptyOrSpaces(vaultUsername)) {
                    parameters.put("VAULT_USERNAME", vaultUsername.trim());
                }
                if (isNotEmptyOrSpaces(userPassAuthPath)) {
                    parameters.put("USERPASS_AUTH_PATH", userPassAuthPath.trim());
                }
            }
            if (authentication == CloudConfigProviderAuthentication.HCP_APPROLE &&
                    isNotEmptyOrSpaces(roleId)) {
                parameters.put("ROLE_ID", roleId.trim());
            }
            if (authentication == CloudConfigProviderAuthentication.HCP_APPROLE &&
                    isNotEmptyOrSpaces(appRoleAuthPath)) {
                parameters.put("APPROLE_AUTH_PATH", appRoleAuthPath.trim());
            }
            if (authentication == CloudConfigProviderAuthentication.HCP_GITHUB &&
                    isNotEmptyOrSpaces(githubAuthPath)) {
                parameters.put("GITHUB_AUTH_PATH", githubAuthPath.trim());
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
            setLocation("");
            return;
        }

        setLocation("project=" + nvl(project, "").trim() +
                ";bucket=" + nvl(bucket, "").trim() +
                ";object=" + nvl(object, "").trim());
    }

    private String getNamedLocationValue(String key) {
        return parseNamedLocation().get(key);
    }

    public void validate(List<String> errors) {
        if (isAzureProvider() &&
                authentication == CloudConfigProviderAuthentication.AZURE_INTERACTIVE &&
                isEmptyOrSpaces(azureClientId)) {
            errors.add("Azure interactive authentication requires client ID");
        }
        if (isAzureProvider() && isAzureServicePrincipalAuthentication(authentication)) {
            if (isEmptyOrSpaces(azureClientId)) {
                errors.add("Azure service principal authentication requires client ID");
            }
            if (isEmptyOrSpaces(azureTenantId)) {
                errors.add("Azure service principal authentication requires tenant ID");
            }
            if (authentication == CloudConfigProviderAuthentication.AZURE_SERVICE_PRINCIPAL_CERTIFICATE &&
                    isEmptyOrSpaces(azureClientCertificatePath)) {
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

    private Map<String, String> parseNamedLocation() {
        Map<String, String> values = new HashMap<>();
        if (isEmptyOrSpaces(location)) return values;

        String[] tokens = location.split(";");
        for (String token : tokens) {
            String[] entry = token.split("=", 2);
            if (entry.length != 2) continue;
            values.put(entry[0].trim().toLowerCase(), entry[1].trim());
        }
        return values;
    }

    public void initialize(String url, DatabaseUrlPattern pattern, Map<String, String> parameters) {
        reset();
        location = pattern.resolveConfigLocation(url);
        profileKey = getParameterIgnoreCase(parameters, "key");
        label = getParameterIgnoreCase(parameters, "label");

        if (pattern.getUrlType() != CONFIG_FILE) return;

        String provider = pattern.resolveConfigProvider(url);
        if (Strings.isEmptyOrSpaces(provider)) return;

        if ("file".equalsIgnoreCase(provider)) {
            apply(ConfigFileSourceType.LOCAL_FILE, null, null, location, profileKey, null);
        } else if ("https".equalsIgnoreCase(provider)) {
            apply(ConfigFileSourceType.HTTPS, null, null, location, profileKey, null);
        } else {
            apply(ConfigFileSourceType.CLOUD_PROVIDER, CloudConfigProviderType.fromSlug(provider), null, location, profileKey, label);
        }

        if (cloudProviderType != null && cloudProviderType.isOci()) {
            authentication = CloudConfigProviderAuthentication.get(getParameterIgnoreCase(parameters, "AUTHENTICATION"));
            ociConfigFile = getParameterIgnoreCase(parameters, "OCI_CONFIG_FILE");
            ociProfile = getParameterIgnoreCase(parameters, "OCI_PROFILE");
        }
        if (isAzureProvider()) {
            authentication = CloudConfigProviderAuthentication.getAzure(
                    getParameterIgnoreCase(parameters, "AUTHENTICATION"),
                    isNotEmptyOrSpaces(getParameterIgnoreCase(parameters, "AZURE_CLIENT_CERTIFICATE_PATH")));
            azureClientId = getParameterIgnoreCase(parameters, "AZURE_CLIENT_ID");
            azureTenantId = getParameterIgnoreCase(parameters, "AZURE_TENANT_ID");
            azureClientCertificatePath = getParameterIgnoreCase(parameters, "AZURE_CLIENT_CERTIFICATE_PATH");
        }
        if (isHashicorpProvider()) {
            authentication = nvl(
                    CloudConfigProviderAuthentication.get(getParameterIgnoreCase(parameters, "authentication")),
                    CloudConfigProviderAuthentication.HCP_DEFAULT);
            vaultAddress = getParameterIgnoreCase(parameters, "VAULT_ADDR");
            vaultNamespace = getParameterIgnoreCase(parameters, "VAULT_NAMESPACE");
            vaultUsername = getParameterIgnoreCase(parameters, "VAULT_USERNAME");
            userPassAuthPath = getParameterIgnoreCase(parameters, "USERPASS_AUTH_PATH");
            roleId = getParameterIgnoreCase(parameters, "ROLE_ID");
            appRoleAuthPath = getParameterIgnoreCase(parameters, "APPROLE_AUTH_PATH");
            githubAuthPath = getParameterIgnoreCase(parameters, "GITHUB_AUTH_PATH");
        }
        if (isRegionConfig()) {
            region = getParameterIgnoreCase(parameters, cloudProviderType.getRegionParameterName());
        }
    }

    private static String getParameterIgnoreCase(Map<String, String> parameters, String key) {
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public void readConfiguration(Element element) {
        ConfigFileSourceType sourceType = getEnum(element, "config-file-source-type", ConfigFileSourceType.LOCAL_FILE);
        CloudConfigProviderType cloudProviderType = getEnum(element, "cloud-config-provider-type", CloudConfigProviderType.class);
        String region = getString(element, "cloud-config-provider-region", null);
        String location = getString(element, "config-location", getString(element, "config-file-path", null));
        String profileKey = getString(element, "config-file-profile-key", null);
        String label = getString(element, "cloud-config-provider-label", null);
        apply(sourceType, cloudProviderType, region, location, profileKey, label);

        CloudConfigProviderAuthentication authentication =
                getEnum(element, "cloud-config-provider-authentication", CloudConfigProviderAuthentication.class);
        if (authentication == null) {
            authentication = getEnum(element, "oci-config-provider-authentication", CloudConfigProviderAuthentication.class);
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
        setEnum(element, "config-file-source-type", sourceType);
        setEnum(element, "cloud-config-provider-type", cloudProviderType);
        setEnum(element, "cloud-config-provider-authentication", authentication);
        setString(element, "oci-config-provider-config-file", ociConfigFile);
        setString(element, "oci-config-provider-profile", ociProfile);
        setString(element, "azure-config-provider-client-id", azureClientId);
        setString(element, "azure-config-provider-tenant-id", azureTenantId);
        setString(element, "azure-config-provider-client-certificate-path", azureClientCertificatePath);
        setString(element, "hashicorp-config-provider-vault-address", vaultAddress);
        setString(element, "hashicorp-config-provider-vault-namespace", vaultNamespace);
        setString(element, "hashicorp-config-provider-vault-username", vaultUsername);
        setString(element, "hashicorp-config-provider-userpass-auth-path", userPassAuthPath);
        setString(element, "hashicorp-config-provider-role-id", roleId);
        setString(element, "hashicorp-config-provider-approle-auth-path", appRoleAuthPath);
        setString(element, "hashicorp-config-provider-github-auth-path", githubAuthPath);
        setString(element, "cloud-config-provider-region", region);
        setString(element, "config-location", location);
        setString(element, "config-file-profile-key", profileKey);
        setString(element, "cloud-config-provider-label", label);
    }

    @Override
    public ConfigProviderInfo clone() {
        ConfigProviderInfo clone = new ConfigProviderInfo();
        clone.sourceType = sourceType;
        clone.cloudProviderType = cloudProviderType;
        clone.authentication = authentication;
        clone.ociConfigFile = ociConfigFile;
        clone.ociProfile = ociProfile;
        clone.region = region;
        clone.location = location;
        clone.profileKey = profileKey;
        clone.label = label;
        clone.azureClientId = azureClientId;
        clone.azureTenantId = azureTenantId;
        clone.azureClientCertificatePath = azureClientCertificatePath;
        clone.vaultAddress = vaultAddress;
        clone.vaultNamespace = vaultNamespace;
        clone.vaultUsername = vaultUsername;
        clone.userPassAuthPath = userPassAuthPath;
        clone.roleId = roleId;
        clone.appRoleAuthPath = appRoleAuthPath;
        clone.githubAuthPath = githubAuthPath;
        clone.credentialConnectionId = credentialConnectionId;
        clone.azureClientSecret.setToken(azureClientSecret);
        clone.azureClientCertificatePassword.setToken(azureClientCertificatePassword);
        clone.hashicorpVaultToken.setToken(hashicorpVaultToken);
        clone.hashicorpVaultPassword.setToken(hashicorpVaultPassword);
        clone.hashicorpAppRoleSecretId.setToken(hashicorpAppRoleSecretId);
        clone.hashicorpGithubToken.setToken(hashicorpGithubToken);
        return clone;
    }

    private static boolean isAzureClientIdAuthentication(CloudConfigProviderAuthentication authentication) {
        return authentication == CloudConfigProviderAuthentication.AZURE_INTERACTIVE ||
                isAzureServicePrincipalAuthentication(authentication);
    }

    private static boolean isAzureServicePrincipalAuthentication(CloudConfigProviderAuthentication authentication) {
        return authentication == CloudConfigProviderAuthentication.AZURE_SERVICE_PRINCIPAL_SECRET ||
                authentication == CloudConfigProviderAuthentication.AZURE_SERVICE_PRINCIPAL_CERTIFICATE;
    }
}
