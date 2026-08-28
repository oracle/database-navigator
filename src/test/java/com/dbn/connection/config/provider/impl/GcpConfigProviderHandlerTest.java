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

package com.dbn.connection.config.provider.impl;

import com.dbn.connection.config.provider.CloudConfigProviderType;
import com.dbn.connection.config.provider.ConfigProviderInfo;
import com.dbn.connection.config.provider.ConfigSourceType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GcpConfigProviderHandlerTest {
    @Test
    public void initializesStorageAttributesFromProviderLocation() {
        ConfigProviderInfo configProvider = new ConfigProviderInfo(null);
        configProvider.setProviderSourceType(ConfigSourceType.CLOUD);
        configProvider.setCloudProviderType(CloudConfigProviderType.GCP_STORAGE);

        new GcpConfigProviderHandler().setProviderLocation(configProvider,
                "project=example-project;bucket=connections;object=connections.json");

        assertEquals("example-project", configProvider.getGcpStorageProject());
        assertEquals("connections", configProvider.getGcpStorageBucket());
        assertEquals("connections.json", configProvider.getGcpStorageObject());
    }

    @Test
    public void createsProviderLocationFromStorageAttributes() {
        assertEquals("project=example-project;bucket=connections;object=connections.json",
                GcpConfigProviderHandler.getStorageLocation(" example-project ", " connections ", " connections.json "));
    }
}
