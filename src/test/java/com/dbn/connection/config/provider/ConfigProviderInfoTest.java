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

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ConfigProviderInfoTest {
    @Test
    public void applyUsesExpectedProviderSlugForEachSourceType() {
        ConfigProviderInfo provider = new ConfigProviderInfo(null);

        provider.apply(ConfigSourceType.FILE, null,
                null, "/tmp/connections.json", null, null);
        assertEquals("file", provider.getProviderSlug());

        provider.apply(ConfigSourceType.URL, null,
                null, "https://example.com/connections.json", null, null);
        assertEquals("https", provider.getProviderSlug());

        for (CloudConfigProviderType type : CloudConfigProviderType.values()) {
            provider.apply(ConfigSourceType.CLOUD, type,
                    null, "config-location", null, null);
            assertEquals(type.getSlug(), provider.getProviderSlug());
        }
    }

    @Test
    public void applyNormalizesOciObjectStorageLocation() {
        ConfigProviderInfo provider = new ConfigProviderInfo(null);

        provider.apply(ConfigSourceType.CLOUD, CloudConfigProviderType.OCI_OBJECT,
                null,
                " https://objectstorage.eu-frankfurt-1.oraclecloud.com/n/example/b/connections/o/connections.json ",
                null, null);

        assertEquals("ociobject", provider.getProviderSlug());
        assertEquals("objectstorage.eu-frankfurt-1.oraclecloud.com/n/example/b/connections/o/connections.json",
                provider.getLocation());
        assertEquals(Map.of(), provider.getUrlParameters(false));
    }

    @Test
    public void applyExportsAwsRegionAndProfileKeyAsUrlParameters() {
        ConfigProviderInfo provider = new ConfigProviderInfo(null);

        provider.apply(ConfigSourceType.CLOUD, CloudConfigProviderType.AWS_S3,
                " eu-west-1 ", "s3.eu-west-1.amazonaws.com/example-bucket/connections.json", "production", null);

        assertEquals("awss3", provider.getProviderSlug());
        assertEquals(Map.of("key", "production", "AWS_REGION", "eu-west-1"), provider.getUrlParameters(false));
    }
}
