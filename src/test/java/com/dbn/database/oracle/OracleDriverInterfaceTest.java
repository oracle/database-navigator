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

package com.dbn.database.oracle;

import org.jdom.Element;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class OracleDriverInterfaceTest {
    private static final String DRIVER_VERSION = "23.26.1.0.0";

    @Test
    public void providerNameIsDerivedFromArtifactId() {
        Assert.assertEquals("Azure", OracleDriverInterface.toProviderName("azure"));
        Assert.assertEquals("Aws Secrets", OracleDriverInterface.toProviderName("aws-secrets"));
        Assert.assertEquals("Gcp", OracleDriverInterface.toProviderName("gcp"));
    }

    @Test
    public void discoveredProviderPackageUsesProviderDriverVersionAndExtensionRoles() {
        Element packageElement = OracleDriverInterface.createOracleProviderPackageElement("ojdbc-provider-aws", DRIVER_VERSION);
        List<Element> libraries = packageElement.getChildren("library");

        Assert.assertEquals("Oracle", packageElement.getAttributeValue("database-type"));
        Assert.assertEquals("AWS", packageElement.getAttributeValue("cloud-config-provider-family"));
        Assert.assertEquals("ojdbc-%s-aws-%s", packageElement.getAttributeValue("id"));
        Assert.assertEquals("Oracle %s + Aws auth %s", packageElement.getAttributeValue("name"));
        Assert.assertEquals(2, libraries.size());
        assertLibrary(libraries.get(0), "ojdbc8", DRIVER_VERSION, "DRIVER", null);
        assertLibrary(libraries.get(1), "ojdbc-provider-aws", "latest", "EXTENSION", "jar");
    }

    @Test
    public void discoveredGcpProviderPackageUsesAuthLabel() {
        Element packageElement = OracleDriverInterface.createOracleProviderPackageElement("ojdbc-provider-gcp", DRIVER_VERSION);
        List<Element> libraries = packageElement.getChildren("library");

        Assert.assertEquals("ojdbc-%s-gcp-%s", packageElement.getAttributeValue("id"));
        Assert.assertEquals("GCP", packageElement.getAttributeValue("cloud-config-provider-family"));
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
