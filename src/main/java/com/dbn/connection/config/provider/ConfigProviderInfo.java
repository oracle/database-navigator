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
import com.dbn.connection.DatabaseUrlPattern;
import com.dbn.connection.config.OciConfigProviderParameters;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

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
import static com.dbn.connection.DatabaseUrlType.CONFIG_FILE;

@Getter
@Setter
@EqualsAndHashCode
public class ConfigProviderInfo implements Cloneable<ConfigProviderInfo> {
    private ConfigFileSourceType sourceType = ConfigFileSourceType.LOCAL_FILE;
    private CloudConfigProviderType cloudProviderType;
    private CloudConfigProviderAuthentication authentication;
    private String ociConfigFile;
    private String ociProfile;
    private String region;
    private String location;
    private String profileKey;

    public void reset() {
        sourceType = ConfigFileSourceType.LOCAL_FILE;
        cloudProviderType = null;
        authentication = null;
        ociConfigFile = null;
        ociProfile = null;
        region = null;
        location = null;
        profileKey = null;
    }

    public void applyOciAuthentication(
            CloudConfigProviderAuthentication authentication,
            String ociConfigFile,
            String ociProfile) {
        this.authentication = authentication;
        this.ociConfigFile = ociConfigFile;
        this.ociProfile = ociProfile;
    }

    public void apply(
            ConfigFileSourceType sourceType,
            CloudConfigProviderType cloudProviderType,
            String region,
            String location,
            String profileKey) {
        this.sourceType = Commons.nvl(sourceType, ConfigFileSourceType.LOCAL_FILE);
        this.cloudProviderType = this.sourceType == ConfigFileSourceType.CLOUD_PROVIDER ? cloudProviderType : null;
        this.region = isRegionConfig() ? region : null;
        this.profileKey = profileKey;
        setLocation(location);
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
                cloudProviderType.isOci() &&
                authentication == CloudConfigProviderAuthentication.OCI_INTERACTIVE;
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

        if (isRegionConfig() && isNotEmptyOrSpaces(region)) {
            parameters.put(cloudProviderType.getRegionParameterName(), region.trim());
        }

        if (includeAuthentication && isCloudProviderConfig() && cloudProviderType != null && cloudProviderType.isOci()) {
            parameters.putAll(OciConfigProviderParameters.build(authentication, ociConfigFile, ociProfile));
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
        profileKey = parameters.get("key");

        if (pattern.getUrlType() != CONFIG_FILE) return;

        String provider = pattern.resolveConfigProvider(url);
        if (Strings.isEmptyOrSpaces(provider)) return;

        if ("file".equalsIgnoreCase(provider)) {
            apply(ConfigFileSourceType.LOCAL_FILE, null, null, location, profileKey);
        } else if ("https".equalsIgnoreCase(provider)) {
            apply(ConfigFileSourceType.HTTPS, null, null, location, profileKey);
        } else {
            apply(ConfigFileSourceType.CLOUD_PROVIDER, CloudConfigProviderType.fromSlug(provider), null, location, profileKey);
        }

        if (cloudProviderType != null && cloudProviderType.isOci()) {
            authentication = CloudConfigProviderAuthentication.get(getParameterIgnoreCase(parameters, "AUTHENTICATION"));
            ociConfigFile = getParameterIgnoreCase(parameters, "OCI_CONFIG_FILE");
            ociProfile = getParameterIgnoreCase(parameters, "OCI_PROFILE");
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
        apply(sourceType, cloudProviderType, region, location, profileKey);

        CloudConfigProviderAuthentication authentication =
                getEnum(element, "cloud-config-provider-authentication", CloudConfigProviderAuthentication.class);
        if (authentication == null) {
            authentication = getEnum(element, "oci-config-provider-authentication", CloudConfigProviderAuthentication.class);
        }

        applyOciAuthentication(
                authentication,
                getString(element, "oci-config-provider-config-file", null),
                getString(element, "oci-config-provider-profile", null));
    }

    public void writeConfiguration(Element element) {
        setEnum(element, "config-file-source-type", sourceType);
        setEnum(element, "cloud-config-provider-type", cloudProviderType);
        setEnum(element, "cloud-config-provider-authentication", authentication);
        setString(element, "oci-config-provider-config-file", ociConfigFile);
        setString(element, "oci-config-provider-profile", ociProfile);
        setString(element, "cloud-config-provider-region", region);
        setString(element, "config-location", location);
        setString(element, "config-file-profile-key", profileKey);
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
        return clone;
    }
}
