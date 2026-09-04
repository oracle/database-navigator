package com.dbn.connection.config.provider.impl;

import com.dbn.common.extension.ExtensionPoint;
import com.dbn.connection.config.provider.CloudConfigProviderFamily;
import com.dbn.connection.config.provider.CloudConfigProviderType;
import com.dbn.connection.config.provider.ConfigProviderInfo;
import com.dbn.connection.config.provider.ConfigSourceType;
import com.intellij.openapi.extensions.ExtensionPointName;

import java.util.List;
import java.util.Map;

public interface CloudConfigProviderHandler extends ExtensionPoint {
    ExtensionPointName<CloudConfigProviderHandler> EP = ExtensionPointName.create("com.dbn.cloudConfigProviderHandler");

    CloudConfigProviderFamily getFamily();

    void addRuntimeSecrets(Map<String, String> parameters, ConfigProviderInfo configProvider);

    void addUrlParameters(Map<String, String> parameters, ConfigProviderInfo configProvider, boolean includeAuthentication);

    boolean managesProviderLocation(ConfigSourceType sourceType, CloudConfigProviderType providerType);

    void setProviderLocation(ConfigProviderInfo configProvider, String providerLocation);

    String getProviderLocation(ConfigProviderInfo configProvider);

    void initialize(ConfigProviderInfo configProvider, Map<String, String> parameters);

    void validate(ConfigProviderInfo configProvider, List<String> errors);
}
