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

package com.dbn.driver.download.metadata;

import org.jdom.Element;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class DriverPackageMetadataDownloaderTest {
    @Test
    public void providerNameIsDerivedFromArtifactId() {
        Assert.assertEquals("Azure", DriverPackageMetadataDownloader.toProviderName("azure"));
        Assert.assertEquals("Aws Secrets", DriverPackageMetadataDownloader.toProviderName("aws-secrets"));
        Assert.assertEquals("Gcp", DriverPackageMetadataDownloader.toProviderName("gcp"));
    }

    @Test
    public void discoveredProviderPackageUsesLatestDriverAndExtensionRoles() {
        Element packageElement = DriverPackageMetadataDownloader.createOracleProviderPackageElement("ojdbc-provider-aws");
        List<Element> libraries = packageElement.getChildren("library");

        Assert.assertEquals("Oracle", packageElement.getAttributeValue("database-type"));
        Assert.assertEquals("ojdbc-%s-aws-%s", packageElement.getAttributeValue("id"));
        Assert.assertEquals("Oracle %s + Aws auth %s", packageElement.getAttributeValue("name"));
        Assert.assertEquals(2, libraries.size());
        assertLibrary(libraries.get(0), "ojdbc8-production", "latest", "DRIVER", "pom");
        assertLibrary(libraries.get(1), "ojdbc-provider-aws", "latest", "EXTENSION", "jar");
    }

    @Test
    public void discoveredGcpProviderPackageUsesAuthLabel() {
        Element packageElement = DriverPackageMetadataDownloader.createOracleProviderPackageElement("ojdbc-provider-gcp");
        List<Element> libraries = packageElement.getChildren("library");

        Assert.assertEquals("ojdbc-%s-gcp-%s", packageElement.getAttributeValue("id"));
        Assert.assertEquals("Oracle %s + Gcp auth %s", packageElement.getAttributeValue("name"));
        assertLibrary(libraries.get(1), "ojdbc-provider-gcp", "latest", "EXTENSION", "jar");
    }

    private static void assertLibrary(Element library, String artifactId, String version, String role, String type) {
        Assert.assertEquals("com.oracle.database.jdbc", library.getAttributeValue("group-id"));
        Assert.assertEquals(artifactId, library.getAttributeValue("artifact-id"));
        Assert.assertEquals(version, library.getAttributeValue("version"));
        Assert.assertEquals(role, library.getAttributeValue("role"));
        Assert.assertEquals(type, library.getAttributeValue("type"));
    }
}
