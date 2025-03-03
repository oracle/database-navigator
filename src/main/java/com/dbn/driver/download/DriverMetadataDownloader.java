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

package com.dbn.driver.download;

import com.dbn.common.download.Downloads;
import com.dbn.common.message.AsyncMessageCollector;
import com.dbn.connection.DatabaseType;
import com.dbn.driver.download.metadata.DriverPackage;
import com.dbn.driver.download.metadata.Library;
import com.intellij.openapi.progress.ProgressIndicator;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

public class DriverMetadataDownloader {
    private AsyncMessageCollector messages;

    @SneakyThrows
    public DriverPackage createDriverPackage(Element element, ProgressIndicator indicator, AsyncMessageCollector messages, float chunk){
        this.messages = messages;
        String id = stringAttribute(element, "id");
        String name = stringAttribute(element, "name");
        String databaseType = stringAttribute(element, "database-type");

        // Collect libraries from each child "library" element
        List<Library> libraries = element.getChildren("library").parallelStream()
                .flatMap(libElement -> createLibrary(libElement).stream()) // Flatten lists of libraries
                .collect(Collectors.toList());
        if(id.contains("%s")) id = String.format(id, libraries.get(0).getVersion());
        indicator.setText("Downloaded Metadata for " + id);
        indicator.setFraction(indicator.getFraction() + chunk);
        // Pattern to find %s occurrences
        int placeholderCount = countPlaceholders(name);

        if (placeholderCount == 1) {
            name = String.format(name, libraries.get(0).getVersion());
        } else if (placeholderCount == 2) {
            // Get the first matching "ojdbc[8|11|17]" library
            Library ojdbcLibrary = libraries.stream()
                    .filter(lib -> lib.getArtifactId().matches("ojdbc(8|11|17)"))
                    .findFirst()
                    .orElse(null);

            // Use the first available library for the second %s
            Library firstLibrary = libraries.get(0);

            if (ojdbcLibrary != null) {
                name = String.format(name, ojdbcLibrary.getVersion(), firstLibrary.getVersion());
            }
        }
        return new DriverPackage(id, name, DatabaseType.resolve(databaseType), libraries);
    }

    private  int countPlaceholders(String str) {
        Matcher matcher = Pattern.compile("%s").matcher(str);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    @SneakyThrows
    private  List<Library> createLibrary(Element element) {
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
                    new Library(groupId, artifactId, version),
                    type
            ); // Return all resolved dependencies
        } else {
            // For type "jar", return a single Library
            return Collections.singletonList(new Library(groupId, artifactId, version));
        }
    }

    private  String ensureVersion(String groupId, String artifactId, String currentVersion) throws Exception{
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
    }

    private  String fetchLatestVersion(List<String> availableVersions) throws Exception {
        return availableVersions.stream()
                .max(Comparator.naturalOrder())
                .orElseThrow(() -> new Exception("No versions found."));
    }

    private  String resolveWildcardVersion(String wildcardVersion, List<String> availableVersions) throws Exception {
        // Convert wildcard version to regex
        String regex = wildcardVersion.replace("*", ".*");
        return availableVersions.stream()
                .filter(v -> v.matches(regex))
                .max(Comparator.naturalOrder()) // Get the latest matching version
                .orElseThrow(() -> new Exception("No matching version found for pattern: " + wildcardVersion));
    }

    private  List<String> fetchAvailableVersions(String groupId, String artifactId) throws Exception {
        // URL for Maven metadata
        String url = "https://repo1.maven.org/maven2/"+groupId.replace('.', '/')+"/"+artifactId+"/maven-metadata.xml";
        // Temporary file to store the downloaded content
        File tempFile = File.createTempFile("maven-metadata", ".xml");
        tempFile.deleteOnExit();

        // Download content to the temporary file
        Downloads.downloadContentToFile(null, url, tempFile);

        // Parse the XML content from the file
        List<String> versions = new ArrayList<>();
        try (FileReader fileReader = new FileReader(tempFile)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(tempFile);
            NodeList versionNodes = document.getElementsByTagName("version");
            for (int i = 0; i < versionNodes.getLength(); i++) {
                String version = versionNodes.item(i).getTextContent();
                if (isStableVersion(version)) {
                    versions.add(version);
                }
            }
        } catch (Exception e) {
            throw new Exception("Error parsing the XML file", e);
        }

        if (!versions.isEmpty()) {
            return versions;
        } else {
            throw new Exception("No versions found for the specified artifact.");
        }
    }

    private  boolean isStableVersion(String version) {
        String unstableKeywords = "SNAPSHOT|RC|M\\d+|BETA|ALPHA";
        return !version.toUpperCase().matches(".*(" + unstableKeywords + ").*");
    }
    // Method to validate the provided version
    private  boolean isValidVersion(String version) {
        // Simple regex to validate semantic versioning (e.g., 1.0.0)
        return version.matches("\\d+(\\.\\d+)*(-[a-zA-Z0-9]+)?");
    }
}
