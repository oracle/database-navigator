package com.dbn.connection.config.provider.impl;

import com.dbn.common.extension.ExtensionPointCache;
import com.dbn.connection.config.provider.CloudConfigProviderFamily;
import com.dbn.connection.config.provider.CloudConfigProviderType;

public class CloudConfigProviderHandlers extends ExtensionPointCache<CloudConfigProviderFamily, CloudConfigProviderHandler> {
    public static final CloudConfigProviderHandlers INSTANCE = new CloudConfigProviderHandlers();

    private CloudConfigProviderHandlers() {
        super(CloudConfigProviderHandler.EP, CloudConfigProviderHandler::getFamily);
    }

    public static CloudConfigProviderHandler get(CloudConfigProviderType providerType) {
        if (providerType == null) return NoOpConfigProviderHandler.INSTANCE;
        CloudConfigProviderFamily family = providerType.getFamily();
        return INSTANCE.find(family);
    }
}
