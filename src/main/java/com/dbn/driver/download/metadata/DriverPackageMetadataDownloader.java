/*
 * Copyright 2025 Oracle and/or its affiliates
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

import com.dbn.common.download.Downloads;
import com.dbn.common.exception.Exceptions;
import com.dbn.common.util.XmlContents;
import com.dbn.connection.DatabaseType;
import com.dbn.driver.download.DependencyParser;
import com.dbn.driver.download.DownloadSession;
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
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.thread.Progress.installThreadInterrupter;
import static com.dbn.nls.NlsResources.txt;

public class DriverPackageMetadataDownloader {
    @SneakyThrows
    public Map<String, DriverPackage> createDriverPackages(DownloadSession session) {
        Element element = XmlContents.fileToElement(getClass(), "driver-packages.xml");
        List<Element> packageElements = element.getChildren("driver-package");
        session.withDownloadSize(packageElements.size());

        List<DriverPackage> driverPackages = packageElements.parallelStream()
                .map(e -> createDriverPackage(e, session))
                .filter(p -> p != null)
                .toList();
        return driverPackages.stream().collect(Collectors.toMap(p -> p.getId(), p -> p));
    }

    private DriverPackage createDriverPackage(Element element, DownloadSession session){
        installThreadInterrupter(session);

        String id = stringAttribute(element, "id");
        String name = stringAttribute(element, "name");
        String databaseType = stringAttribute(element, "database-type");

        // Collect libraries from each child "library" element
        List<Library> libraries = element.getChildren("library").parallelStream()
                .flatMap(libElement -> createLibrary(libElement, session).stream()) // Flatten lists of libraries
                .collect(Collectors.toList());
        int placeholderCount = countPlaceholders(id);
        if (libraries.isEmpty()) return null;

        id = getFormattedString(id, libraries, placeholderCount, true);
        // Pattern to find %s occurrences
        placeholderCount = countPlaceholders(name);

        name = getFormattedString(name, libraries, placeholderCount, false);
        DriverPackage driverPackage = new DriverPackage(id, name, DatabaseType.resolve(databaseType), libraries);

        session.setText("Downloading metadata for " + id + " ...");
        session.countDown();
        session.updateProgress();
        return driverPackage;
    }

    private String getFormattedString(String s, List<Library> libraries, int placeholderCount, boolean abridged) {
        if (placeholderCount == 1) {
             s = String.format(s, libraries.get(0).getVersion());
        } else if (placeholderCount == 2) {
            // Get the first matching "ojdbc[8|11|17]" library
            Library ojdbcLibrary = libraries.stream()
                    .filter(lib -> lib.getArtifactId().matches("ojdbc(8|11|17)"))
                    .findFirst()
                    .orElse(null);

            // Use the first available library for the second %s
            Library firstLibrary = libraries.get(0);
            if (ojdbcLibrary != null) {
                String ojdbcVersion = abridged?shortenVersion(ojdbcLibrary.getVersion()):ojdbcLibrary.getVersion();
                String extensionVersion = firstLibrary.getVersion();
                s = String.format(s, ojdbcVersion, extensionVersion);
            }
        }
        return s;
    }

    private  int countPlaceholders(String str) {
        Matcher matcher = Pattern.compile("%s").matcher(str);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
    public static String shortenVersion(String version) {
        String[] splitVersion = version.split("\\.");
        if (splitVersion.length <= 3) {
            return version;
        }
        return String.join(".", java.util.Arrays.copyOfRange(splitVersion, 0, 3));
    }
    @SneakyThrows
    private  List<Library> createLibrary(Element element, DownloadSession session) {
        installThreadInterrupter(session);

        String groupId = stringAttribute(element, "group-id");
        String artifactId = stringAttribute(element, "artifact-id");
        String version = stringAttribute(element, "version");
        boolean toResolve = booleanAttribute(element, "toResolve", false);
        String type = stringAttribute(element, "type");
        if (type == null) type = "jar";
        // Resolve the version if not explicitly provided
        version = ensureVersion(groupId, artifactId, version, session);

        Library library = new Library(groupId, artifactId, version);
        readChecksums(element, library);
        if (toResolve) {
            // Resolve dependencies for non-jar types
            try {
                List<Library> libraries = DependencyParser.resolveDependencies(library, type, session);
                copyChecksums(library, libraries);
                return libraries; // Return all resolved dependencies
            } catch (Throwable e) {
                e = Exceptions.rootCauseOf(e);
                session.addErrorMessage(txt("msg.connection.error.FailedToDownloadLibrary", library.getLibraryId(), e.getMessage()));
                return Collections.emptyList();
            }

        } else {
            // For type "jar", return a single Library
            return Collections.singletonList(library);
        }
    }

    private void copyChecksums(Library source, List<Library> libraries) {
        if (source.getChecksums().isEmpty()) return;

        libraries.stream()
                .filter(l -> Objects.equals(l.getGroupId(), source.getGroupId()))
                .filter(l -> Objects.equals(l.getArtifactId(), source.getArtifactId()))
                .filter(l -> Objects.equals(l.getVersion(), source.getVersion()))
                .forEach(l -> l.getChecksums().addAll(source.getChecksums()));
    }

    private void readChecksums(Element element, Library library) {
        for (Element checksumElement : childrenOf(element, "checksum")) {
            LibraryChecksum checksum = new LibraryChecksum();
            checksum.readState(checksumElement);
            library.getChecksums().add(checksum);
        }
    }

    private  String ensureVersion(String groupId, String artifactId, String currentVersion, DownloadSession session) throws Exception{
        if (currentVersion != null && isValidVersion(currentVersion)) {
            return currentVersion;
        }

        // Fetch all available versions
        List<String> availableVersions = fetchAvailableVersions(groupId, artifactId, session);
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

    private  List<String> fetchAvailableVersions(String groupId, String artifactId, ProgressIndicator indicator) throws Exception {
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

            // XML External Entity Injection (fortify recommendations)
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

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
