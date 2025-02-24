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

import com.dbn.common.checksum.Checksum;
import com.dbn.common.checksum.ChecksumType;
import com.dbn.common.util.XmlContents;
import com.dbn.connection.DatabaseType;
import com.dbn.driver.packages.parser.DependencyParser;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.platform.templates.github.DownloadUtil;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Files.getPluginDeploymentRoot;
import static com.dbn.common.util.Lists.convertParallel;
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
    private static DriverPackageBundle INSTANCE;
    private final List<DriverPackage> driverPackages;

    @SneakyThrows
    private DriverPackageBundle(ProgressIndicator indicator) {
        Element element = XmlContents.fileToElement(getClass(), "driver-packages.xml");
        List<Element> packageElements = element.getChildren("driver-package");

        driverPackages = unmodifiableList(convertParallel(packageElements, e-> DriverPackageBundle.createDriverPackage(e, indicator, (float) 1 /packageElements.size())));
    }

    public static synchronized void initialize(ProgressIndicator indicator) {
        if (INSTANCE == null) {
            INSTANCE = new DriverPackageBundle(indicator);
        }
    }
    private static final String DRIVER_PACKAGES_PATH = getPluginDeploymentRoot().getPath()+"/driver-packages/checksums";

    public static List<DriverPackage> driverPackages(){
        if (INSTANCE == null) {
            throw new IllegalStateException("DriverPackageBundle is not initialized. Call initialize() first.");
        }
        if(!DriverDownloadManager.getInstance().getPackagesStatus().isEmpty()){
            for (DriverPackage driverPackage : INSTANCE.driverPackages) {
                String packageId = driverPackage.getId();
                if(driverPackage.getPath()==null) continue;
                File packageDir = new File(driverPackage.getPath());
                File checksumFile = new File(DRIVER_PACKAGES_PATH, packageId + ".txt");

                if (!checksumFile.exists()) {
                    for(Library library :driverPackage.getLibraries()){
                        String jarId = library.getArtifactId()+"-"+library.getVersion();
                        DriverDownloadManager.getInstance().updateJarDownloadStatus(packageId, jarId, DownloadStatus.NEW);
                    }
                    continue;
                }

                try (BufferedReader reader = new BufferedReader(new FileReader(checksumFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(" ");
                        if (parts.length != 2) continue;

                        String artifactIdVersion = parts[0]; // Format: artifactId-version
                        String expectedChecksum = parts[1];

                        File jarFile = new File(packageDir, artifactIdVersion+".jar");

                        if (!jarFile.exists() || !verifyChecksum(jarFile, expectedChecksum, packageId, artifactIdVersion)) {
                            DriverDownloadManager.getInstance().cleanupPackage(packageId);
                            break;
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error reading checksum file for package: " + packageId);
                }
            }

        }

        return INSTANCE.driverPackages;
    }
    private static boolean verifyChecksum(File outputFile, String expectedChecksum, String packageId, String artifactIdAndVersion) {
            // Compute actual SHA-1 checksum
            String actualChecksum = Checksum.fromFileContent(outputFile, ChecksumType.SHA_1);

            if (expectedChecksum.equalsIgnoreCase(actualChecksum)) {
                return true;
            } else {
                System.err.println("Checksum verification failed for " + artifactIdAndVersion +
                        "! Expected: " + expectedChecksum + ", Actual: " + actualChecksum);
                return false;
            }
    }


    public static List<DriverPackage> driverPackages(DatabaseType databaseType, ProgressIndicator indicator){
        initialize(indicator);
        return driverPackages().stream().filter(dp -> dp.getDatabaseType() == databaseType || dp.getDatabaseType() == DatabaseType.GENERIC).collect(Collectors.toList());
    }

    public static List<DriverPackage> getDownloadedDriverPackage() {
        if (INSTANCE == null) return new ArrayList<>();
        try {
            return driverPackages().stream()
                    .filter(driverPackage -> DriverDownloadManager.getInstance().isPackageDownloaded(driverPackage.getId()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    @SneakyThrows
    private static DriverPackage createDriverPackage(Element element, ProgressIndicator indicator, float chunk){
        String id = stringAttribute(element, "id");
        String name = stringAttribute(element, "name");
        String databaseType = stringAttribute(element, "database-type");

        // Collect libraries from each child "library" element
        List<Library> libraries = element.getChildren("library").parallelStream()
                .flatMap(libElement -> createLibrary(libElement).stream()) // Flatten lists of libraries
                .collect(Collectors.toList());
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

    private static int countPlaceholders(String str) {
        Matcher matcher = Pattern.compile("%s").matcher(str);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    @SneakyThrows
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
                    new Library(groupId, artifactId, version),
                    type
            ); // Return all resolved dependencies
        } else {
            // For type "jar", return a single Library
            return Collections.singletonList(new Library(groupId, artifactId, version));
        }
    }


    @NotNull
    public static DriverPackage getDriverPackage(String packageId){
        Optional<DriverPackage> driverPackage = driverPackages().stream().filter(p -> p.getId().equals(packageId)).findFirst();
        return driverPackage.get();
    }

    private static String ensureVersion(String groupId, String artifactId, String currentVersion) throws Exception{
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
        // URL for Maven metadata
        String url = "https://repo1.maven.org/maven2/"+groupId.replace('.', '/')+"/"+artifactId+"/maven-metadata.xml";
        // Temporary file to store the downloaded content
        File tempFile = File.createTempFile("maven-metadata", ".xml");
        tempFile.deleteOnExit();

        // Download content to the temporary file
        DownloadUtil.downloadContentToFile(null, url, tempFile);

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