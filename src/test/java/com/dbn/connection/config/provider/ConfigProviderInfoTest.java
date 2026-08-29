/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.connection.config.provider;

import com.dbn.connection.config.provider.impl.AwsConfigProviderHandler;
import com.dbn.connection.config.provider.impl.AzureConfigProviderHandler;
import com.dbn.connection.config.provider.impl.CloudConfigProviderHandlers;
import com.dbn.connection.config.provider.impl.GcpConfigProviderHandler;
import com.dbn.connection.config.provider.impl.HashicorpConfigProviderHandler;
import com.dbn.connection.config.provider.impl.OciConfigProviderHandler;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConfigProviderInfoTest {
    static {
        CloudConfigProviderHandlers handlers = CloudConfigProviderHandlers.INSTANCE;
        handlers.register(new AwsConfigProviderHandler());
        handlers.register(new AzureConfigProviderHandler());
        handlers.register(new GcpConfigProviderHandler());
        handlers.register(new HashicorpConfigProviderHandler());
        handlers.register(new OciConfigProviderHandler());
    }

    @Test
    public void usesExpectedProviderSlugForEachSourceType() {
        ConfigProviderInfo provider = new ConfigProviderInfo(null);

        provider.setProviderSourceType(ConfigSourceType.FILE);
        provider.setCloudProviderType(null);
        provider.setProviderLocation("/tmp/connections.json");
        assertEquals("file", provider.getProviderSlug());

        provider.setProviderSourceType(ConfigSourceType.URL);
        provider.setCloudProviderType(null);
        provider.setProviderLocation("https://example.com/connections.json");
        assertEquals("https", provider.getProviderSlug());

        for (CloudConfigProviderType type : CloudConfigProviderType.values()) {
            provider.setProviderSourceType(ConfigSourceType.CLOUD);
            provider.setCloudProviderType(type);
            provider.setProviderLocation("config-location");
            assertEquals(type.getSlug(), provider.getProviderSlug());
        }
    }

    @Test
    public void normalizesOciObjectStorageLocation() {
        ConfigProviderInfo provider = new ConfigProviderInfo(null);

        provider.setProviderSourceType(ConfigSourceType.CLOUD);
        provider.setCloudProviderType(CloudConfigProviderType.OCI_OBJECT);
        provider.setProviderLocation(" https://objectstorage.eu-frankfurt-1.oraclecloud.com/n/example/b/connections/o/connections.json ");

        assertEquals("ociobject", provider.getProviderSlug());
        assertEquals("objectstorage.eu-frankfurt-1.oraclecloud.com/n/example/b/connections/o/connections.json",
                provider.getProviderLocation());
    }

}
