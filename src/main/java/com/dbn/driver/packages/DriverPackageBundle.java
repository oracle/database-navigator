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

package com.dbn.driver.packages;

import com.dbn.common.util.XmlContents;
import com.dbn.connection.DatabaseType;
import com.dbn.driver.packages.parser.DependencyParser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.platform.templates.github.DownloadUtil;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Lists.convert;
import static java.util.Collections.unmodifiableList;

/**
 * DriverPackages holds metadata for supported database drivers.
 * The information is parsed from a driver-packages.xml file that includes the following structure:
 * <pre>
 * {@code
 * <driver-packages>
 *     <driver-package id="oracle-23.3-standard" name="Oracle 23.3" database-type="ORACLE">
 *         <library group-id="javax.resource" artifact-id="connector-api" version="1.5"/>
 *         <library group-id="oracle.jdbc" artifact-id="ojdbc8" version="23.3.0.23.09"/>
 *     </driver-package>
 *     <driver-package id="mysql-8.0-standard" name="MySQL 8.0.33" database-type="MYSQL">
 *         <library group-id="mysql" artifact-id="mysql-connector-j" version="8.0.33"/>
 *     </driver-package>
 *     ...
 * </driver-packages>
 * }
 * </pre>
 * It supports the association of database types and corresponding libraries.
 * Libraries are specified with groupId, artifactId, and version attributes.
 *
 * @author Ayoub Aarrasse
 */
public class DriverPackageBundle {
    private static final DriverPackageBundle INSTANCE = new DriverPackageBundle();
    private final List<DriverPackage> driverPackages;

    @SneakyThrows
    private DriverPackageBundle() {
        Element element = XmlContents.fileToElement(getClass(), "driver-packages.xml");
        List<Element> packageElements = element.getChildren("driver-package");

        driverPackages = unmodifiableList(convert(packageElements, DriverPackageBundle::createDriverPackage));
    }

    private static DriverPackage createDriverPackage(Element element) {
        String id = stringAttribute(element, "id");
        String name = stringAttribute(element, "name");
        String databaseType = stringAttribute(element, "database-type");

        // Collect libraries from each child "library" element
        List<Library> libraries = element.getChildren("library").stream()
                .flatMap(libElement -> createLibrary(libElement).stream()) // Flatten lists of libraries
                .collect(Collectors.toList());

        return new DriverPackage(id, name, DatabaseType.resolve(databaseType), libraries);
    }

    private static List<Library> createLibrary(Element element) {
        String groupId = stringAttribute(element, "group-id");
        String artifactId = stringAttribute(element, "artifact-id");
        String version = stringAttribute(element, "version");
        boolean toResolve = booleanAttribute(element, "toResolve", false);
        String type = stringAttribute(element, "type");
        if (type == null) type = "jar";
        // Resolve the version if not explicitly provided
        version = ensureVersion(groupId, artifactId, version);

        if (toResolve) {
            // Resolve dependencies for non-jar types
            return DependencyParser.resolveDependencies(
                    new Library(groupId, artifactId, version, null, null),
                    type
            ); // Return all resolved dependencies
        } else {
            // For type "jar", return a single Library
            return Collections.singletonList(new Library(groupId, artifactId, version));
        }
    }

    public static List<DriverPackage> driverPackages() {
        return INSTANCE.driverPackages;
    }

    @NotNull
    public static DriverPackage getDriverPackage(String packageId) {
        Optional<DriverPackage> driverPackage = driverPackages().stream().filter(p -> p.getId().equals(packageId)).findFirst();
        return driverPackage.get();
    }

    private static String ensureVersion(String groupId, String artifactId, String currentVersion) {
        try {
            if (currentVersion != null && isValidVersion(currentVersion)) {
                return currentVersion;
            }

            // Fetch all available versions
            List<String> availableVersions = fetchAvailableVersions(groupId, artifactId);

            if (currentVersion != null && currentVersion.contains("*")) {
                return resolveWildcardVersion(currentVersion, availableVersions);
            } else {
                return fetchLatestVersion(availableVersions);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    private static String fetchLatestVersion(List<String> availableVersions) throws Exception {
        return availableVersions.stream()
                .max(Comparator.naturalOrder())
                .orElseThrow(() -> new Exception("No versions found."));
    }

    private static String resolveWildcardVersion(String wildcardVersion, List<String> availableVersions) throws Exception {
        // Convert wildcard version to regex
        String regex = wildcardVersion.replace("*", ".*");
        return availableVersions.stream()
                .filter(v -> v.matches(regex))
                .max(Comparator.naturalOrder()) // Get the latest matching version
                .orElseThrow(() -> new Exception("No matching version found for pattern: " + wildcardVersion));
    }

    private static List<String> fetchAvailableVersions(String groupId, String artifactId) throws Exception {
        // URL encode the groupId and artifactId
        String encodedGroupId = URLEncoder.encode(groupId, StandardCharsets.UTF_8);
        String encodedArtifactId = URLEncoder.encode(artifactId, StandardCharsets.UTF_8);
        String url = String.format(
                "https://search.maven.org/solrsearch/select?q=g:%%22%s%%22+AND+a:%%22%s%%22&core=gav&wt=json",
                encodedGroupId, encodedArtifactId);

        // Temporary file to store the downloaded content
        File tempFile = File.createTempFile("maven-response", ".json");
        tempFile.deleteOnExit();

        // Download content to the temporary file
        DownloadUtil.downloadContentToFile(null, url, tempFile);

        // Parse the JSON content from the file
        JsonObject jsonObject = JsonParser.parseReader(new java.io.FileReader(tempFile)).getAsJsonObject();
        JsonObject responseObject = jsonObject.getAsJsonObject("response");
        JsonArray docsArray = responseObject.getAsJsonArray("docs");

        if (!docsArray.isEmpty()) {
            List<String> versions = new ArrayList<>();
            for (JsonElement docElement : docsArray) {
                JsonObject doc = docElement.getAsJsonObject();
                String version = doc.get("v").getAsString();
                if (isStableVersion(version)) {
                    versions.add(version);
                }
            }
            return versions;
        } else {
            throw new Exception("No documents found for the specified artifact.");
        }
    }
    private static boolean isStableVersion(String version) {
        String unstableKeywords = "SNAPSHOT|RC|M\\d+|BETA|ALPHA";
        return !version.toUpperCase().matches(".*(" + unstableKeywords + ").*");
    }
    // Method to validate the provided version
    private static boolean isValidVersion(String version) {
        // Simple regex to validate semantic versioning (e.g., 1.0.0)
        return version.matches("\\d+(\\.\\d+)*(-[a-zA-Z0-9]+)?");
    }
}