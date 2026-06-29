/*
 * Copyright 2024 Oracle and/or its affiliates
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

import com.dbn.common.download.Downloads;
import com.dbn.database.interfaces.DatabaseDriverInterface;
import com.dbn.driver.download.MavenRepositories;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.driver.download.metadata.LibraryRole.DRIVER;
import static com.dbn.driver.download.metadata.LibraryRole.EXTENSION;

@Slf4j
public class OracleDriverInterface implements DatabaseDriverInterface {
    private static final String LATEST_VERSION = "latest";
    private static final String ORACLE_JDBC_GROUP_ID = "com.oracle.database.jdbc";
    private static final String ORACLE_DRIVER_ARTIFACT_ID = "ojdbc8-production";
    private static final String OJDBC_PROVIDER_PREFIX = "ojdbc-provider-";
    private static final String OJDBC_PROVIDER_COMMON = "ojdbc-provider-common";

    @Override
    public List<Element> discoverDriverPackages(List<Element> packageElements) {
        try {
            Set<String> declaredArtifacts = getDeclaredOracleProviderArtifacts(packageElements);
            return fetchOracleProviderArtifactIds()
                    .stream()
                    .filter(artifactId -> !declaredArtifacts.contains(artifactId))
                    .map(OracleDriverInterface::createOracleProviderPackageElement)
                    .toList();
        } catch (Throwable e) {
            log.warn("Oracle JDBC provider artifact discovery failed", e);
            return List.of();
        }
    }

    private Set<String> getDeclaredOracleProviderArtifacts(List<Element> packageElements) {
        return packageElements.stream()
                .flatMap(e -> e.getChildren("library").stream())
                .map(e -> stringAttribute(e, "artifact-id"))
                .filter(Objects::nonNull)
                .filter(OracleDriverInterface::isOracleProviderArtifact)
                .collect(Collectors.toSet());
    }

    private List<String> fetchOracleProviderArtifactIds() throws Exception {
        String url = MavenRepositories.CENTRAL_URL + "/" + ORACLE_JDBC_GROUP_ID.replace('.', '/') + "/";
        File tempFile = File.createTempFile("maven-artifacts", ".html");
        tempFile.deleteOnExit();

        Downloads.downloadContentToFile(null, url, tempFile);

        List<String> artifactIds = new ArrayList<>();
        String html = Files.readString(tempFile.toPath(), StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile("href=\"([^\"]+)/\"").matcher(html);
        while (matcher.find()) {
            String artifactId = matcher.group(1);
            if (artifactId.equals("..")) continue;
            if (artifactId.equals(OJDBC_PROVIDER_COMMON)) continue;
            if (!isOracleProviderArtifact(artifactId)) continue;
            artifactIds.add(artifactId);
        }
        artifactIds.sort(String::compareTo);
        return artifactIds;
    }

    private static Element createOracleProviderPackageElement(String artifactId) {
        String providerId = stripOracleProviderPrefix(artifactId);
        String providerName = toProviderName(providerId);

        Element packageElement = new Element("driver-package");
        packageElement.setAttribute("database-type", "Oracle");
        packageElement.setAttribute("id", "ojdbc-%s-" + providerId + "-%s");
        packageElement.setAttribute("name", "Oracle %s + " + providerName + " auth %s");
        packageElement.addContent(createOracleDriverLibraryElement());
        packageElement.addContent(createOracleProviderLibraryElement(artifactId));
        return packageElement;
    }

    private static Element createOracleDriverLibraryElement() {
        Element libraryElement = new Element("library");
        libraryElement.setAttribute("artifact-id", ORACLE_DRIVER_ARTIFACT_ID);
        libraryElement.setAttribute("group-id", ORACLE_JDBC_GROUP_ID);
        libraryElement.setAttribute("version", LATEST_VERSION);
        libraryElement.setAttribute("role", DRIVER.name());
        libraryElement.setAttribute("type", "pom");
        return libraryElement;
    }

    private static Element createOracleProviderLibraryElement(String artifactId) {
        Element libraryElement = new Element("library");
        libraryElement.setAttribute("artifact-id", artifactId);
        libraryElement.setAttribute("group-id", ORACLE_JDBC_GROUP_ID);
        libraryElement.setAttribute("version", LATEST_VERSION);
        libraryElement.setAttribute("role", EXTENSION.name());
        libraryElement.setAttribute("type", "jar");
        return libraryElement;
    }

    private static boolean isOracleProviderArtifact(String artifactId) {
        return artifactId.startsWith(OJDBC_PROVIDER_PREFIX);
    }

    private static String stripOracleProviderPrefix(String artifactId) {
        if (artifactId.startsWith(OJDBC_PROVIDER_PREFIX)) {
            return artifactId.substring(OJDBC_PROVIDER_PREFIX.length());
        }
        return artifactId;
    }

    private static String toProviderName(String providerId) {
        return Pattern.compile("(^|-)([a-z])")
                .matcher(providerId)
                .replaceAll(match -> match.group(1).replace("-", " ") + match.group(2).toUpperCase());
    }
}
