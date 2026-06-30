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
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileReader;
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
    private static final String ORACLE_DRIVER_ARTIFACT_ID = "ojdbc8";
    private static final String ORACLE_PROVIDER_PARENT_ARTIFACT_ID = "ojdbc-extensions";
    private static final String OJDBC_PROVIDER_PREFIX = "ojdbc-provider-";
    private static final String OJDBC_PROVIDER_COMMON = "ojdbc-provider-common";

    @Override
    public List<Element> discoverDriverPackages(List<Element> packageElements) {
        try {
            Set<String> declaredArtifacts = getDeclaredOracleProviderArtifacts(packageElements);
            return fetchOracleProviderArtifactIds()
                    .stream()
                    .filter(artifactId -> !declaredArtifacts.contains(artifactId))
                    .map(this::createOracleProviderPackageElement)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Throwable e) {
            log.warn("Oracle JDBC provider artifact discovery failed", e);
            return List.of();
        }
    }

    private Set<String> getDeclaredOracleProviderArtifacts(List<Element> packageElements) {
        return packageElements.stream()
                .flatMap(e -> e.getChildren("library").stream())
                .filter(e -> LATEST_VERSION.equalsIgnoreCase(stringAttribute(e, "version")))
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

    private Element createOracleProviderPackageElement(String artifactId) {
        try {
            return createOracleProviderPackageElement(artifactId, fetchProviderDriverVersion(artifactId));
        } catch (Exception e) {
            log.warn("Oracle JDBC provider package discovery failed for artifact '{}'", artifactId, e);
            return null;
        }
    }

    private static Element createOracleProviderPackageElement(String artifactId, String driverVersion) {
        String providerId = stripOracleProviderPrefix(artifactId);
        String providerName = toProviderName(providerId);

        Element packageElement = new Element("driver-package");
        packageElement.setAttribute("database-type", "Oracle");
        packageElement.setAttribute("id", "ojdbc-%s-" + providerId + "-%s");
        packageElement.setAttribute("name", "Oracle %s + " + providerName + " auth %s");
        packageElement.addContent(createOracleDriverLibraryElement(driverVersion));
        packageElement.addContent(createOracleProviderLibraryElement(artifactId));
        return packageElement;
    }

    private static Element createOracleDriverLibraryElement(String version) {
        Element libraryElement = new Element("library");
        libraryElement.setAttribute("artifact-id", ORACLE_DRIVER_ARTIFACT_ID);
        libraryElement.setAttribute("group-id", ORACLE_JDBC_GROUP_ID);
        libraryElement.setAttribute("version", version);
        libraryElement.setAttribute("role", DRIVER.name());
        libraryElement.setAttribute("type", "jar");
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

    private static String fetchProviderDriverVersion(String artifactId) {
        try {
            String providerVersion = fetchLatestProviderVersion(artifactId);
            String parentVersion = fetchProviderParentVersion(artifactId, providerVersion);
            return fetchParentDriverVersion(parentVersion);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve Oracle JDBC driver version for provider " + artifactId, e);
        }
    }

    private static String fetchLatestProviderVersion(String artifactId) throws Exception {
        ElementValueReader reader = downloadXml(ORACLE_JDBC_GROUP_ID, artifactId, "maven-metadata.xml");
        String version = reader.first("release");
        if (version == null) version = reader.first("latest");
        if (version == null) throw new Exception("Missing release version for " + artifactId);
        return version;
    }

    private static String fetchProviderParentVersion(String artifactId, String providerVersion) throws Exception {
        ElementValueReader reader = downloadXml(ORACLE_JDBC_GROUP_ID, artifactId, artifactId + "-" + providerVersion + ".pom", providerVersion);
        String parentVersion = reader.parentVersion();
        if (parentVersion == null) throw new Exception("Missing parent version for " + artifactId + ":" + providerVersion);
        return parentVersion;
    }

    private static String fetchParentDriverVersion(String parentVersion) throws Exception {
        ElementValueReader reader = downloadXml(ORACLE_JDBC_GROUP_ID, ORACLE_PROVIDER_PARENT_ARTIFACT_ID, ORACLE_PROVIDER_PARENT_ARTIFACT_ID + "-" + parentVersion + ".pom", parentVersion);
        String jdbcVersion = reader.property("jdbc.version");
        if (jdbcVersion != null) return jdbcVersion;

        jdbcVersion = reader.managedDependencyVersion(ORACLE_JDBC_GROUP_ID, ORACLE_DRIVER_ARTIFACT_ID);
        if (jdbcVersion == null) throw new Exception("Missing Oracle JDBC driver version in " + ORACLE_PROVIDER_PARENT_ARTIFACT_ID + ":" + parentVersion);
        return jdbcVersion;
    }

    private static ElementValueReader downloadXml(String groupId, String artifactId, String fileName) throws Exception {
        return downloadXml(groupId, artifactId, fileName, null);
    }

    private static ElementValueReader downloadXml(String groupId, String artifactId, String fileName, String version) throws Exception {
        String url = MavenRepositories.CENTRAL_URL + "/" + groupId.replace('.', '/') + "/" + artifactId + "/" + (version == null ? "" : version + "/") + fileName;
        File tempFile = File.createTempFile("maven-xml", ".xml");
        tempFile.deleteOnExit();

        Downloads.downloadContentToFile(null, url, tempFile);
        return new ElementValueReader(tempFile);
    }

    private static final class ElementValueReader {
        private final Document document;

        private ElementValueReader(File file) throws Exception {
            try (FileReader fileReader = new FileReader(file)) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

                DocumentBuilder builder = factory.newDocumentBuilder();
                document = builder.parse(new org.xml.sax.InputSource(fileReader));
            }
        }

        private String first(String tagName) {
            NodeList nodes = document.getElementsByTagName(tagName);
            if (nodes.getLength() == 0) return null;
            return nodes.item(0).getTextContent();
        }

        private String parentVersion() {
            NodeList parentNodes = document.getElementsByTagName("parent");
            if (parentNodes.getLength() == 0) return null;

            Node parent = parentNodes.item(0);
            NodeList children = parent.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (!"version".equals(child.getNodeName())) continue;
                return child.getTextContent();
            }
            return null;
        }

        private String property(String propertyName) {
            return first(propertyName);
        }

        private String managedDependencyVersion(String groupId, String artifactId) {
            NodeList dependencyNodes = document.getElementsByTagName("dependency");
            for (int i = 0; i < dependencyNodes.getLength(); i++) {
                Node dependency = dependencyNodes.item(i);
                String dependencyGroupId = childText(dependency, "groupId");
                String dependencyArtifactId = childText(dependency, "artifactId");
                if (!Objects.equals(groupId, dependencyGroupId)) continue;
                if (!Objects.equals(artifactId, dependencyArtifactId)) continue;

                return childText(dependency, "version");
            }
            return null;
        }

        private static String childText(Node node, String name) {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (!name.equals(child.getNodeName())) continue;
                return child.getTextContent();
            }
            return null;
        }
    }
}
