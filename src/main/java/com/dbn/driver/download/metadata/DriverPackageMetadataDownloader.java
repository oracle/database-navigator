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
import com.dbn.driver.download.DownloadSession;
import com.dbn.driver.download.MavenRepositories;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jdom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.thread.Progress.installThreadInterrupter;
import static com.dbn.driver.download.DependencyParser.resolveDependencies;
import static com.dbn.driver.download.metadata.LibraryRole.DRIVER;
import static com.dbn.driver.download.metadata.LibraryRole.EXTENSION;
import static com.dbn.nls.NlsResources.txt;

@Slf4j
public class DriverPackageMetadataDownloader {
    private static final String LATEST_VERSION = "latest";
    private static final int MAX_LATEST_VERSION_ATTEMPTS = 3;
    private static final String ORACLE_JDBC_GROUP_ID = "com.oracle.database.jdbc";
    private static final String ORACLE_DRIVER_ARTIFACT_ID = "ojdbc8-production";
    private static final String OJDBC_PROVIDER_PREFIX = "ojdbc-provider-";
    private static final String OJDBC_PROVIDER_COMMON = "ojdbc-provider-common";

    @SneakyThrows
    public Map<String, DriverPackage> createDriverPackages(DownloadSession session, DatabaseType databaseType) {
        Element element = XmlContents.fileToElement(getClass(), "driver-packages.xml");
        List<Element> packageElements = new ArrayList<>(element
                .getChildren("driver-package")
                .stream()
                .filter(e -> DatabaseType.resolve(stringAttribute(e, "database-type")) == databaseType)
                .toList());
        packageElements.addAll(createDiscoveredDriverPackageElements(databaseType, packageElements));
        session.withDownloadSize(packageElements.size());

        List<DriverPackage> driverPackages = packageElements.parallelStream()
                .map(e -> createDriverPackage(e, session))
                .filter(p -> p != null)
                .toList();
        driverPackages = removeDuplicatePinnedPackages(driverPackages);
        return driverPackages.stream().collect(Collectors.toMap(p -> p.getId(), p -> p));
    }

    private DriverPackage createDriverPackage(Element element, DownloadSession session){
        installThreadInterrupter(session);

        String id = stringAttribute(element, "id");
        String name = stringAttribute(element, "name");
        String databaseType = stringAttribute(element, "database-type");
        boolean latestPackage = isLatestPackage(element);

        List<ResolvedLibrary> resolvedLibraries = element
                .getChildren("library")
                .parallelStream()
                .map(e -> resolveLibraryMetadata(e, session))
                .toList();
        if (resolvedLibraries.stream().anyMatch(l -> l.libraries().isEmpty())) return null;

        List<Library> libraries = resolvedLibraries
                .stream()
                .flatMap(l -> l.libraries().stream())
                .collect(Collectors.toList());

        int placeholderCount = countPlaceholders(id);
        if (libraries.isEmpty()) return null;

        id = getFormattedString(id, libraries, resolvedLibraries, placeholderCount, true);
        // Pattern to find %s occurrences
        placeholderCount = countPlaceholders(name);

        name = getFormattedString(name, libraries, resolvedLibraries, placeholderCount, false);
        DriverPackage driverPackage = new DriverPackage(id, name, DatabaseType.resolve(databaseType), libraries);
        driverPackage.setLatest(latestPackage);
        if (latestPackage) {
            driverPackage.setDetailsResolved(false);
            driverPackage.setSourceId(stringAttribute(element, "id"));
            driverPackage.setSourceName(stringAttribute(element, "name"));
            driverPackage.setSourceLibraryElements(new ArrayList<>(element.getChildren("library")));
        }

        session.setText(txt("prc.connection.text.DownloadingDriverPackageMetadata", id));
        session.countDown();
        session.updateProgress();
        return driverPackage;
    }

    public synchronized DriverPackage resolveDriverPackageDetails(DriverPackage driverPackage, DownloadSession session) {
        if (driverPackage.isDetailsAvailable()) return driverPackage;
        if (!driverPackage.hasSourceMetadata()) {
            driverPackage.setDetailsResolved(true);
            return driverPackage;
        }

        driverPackage.setDetailsResolving(true);
        try {
            List<ResolvedLibrary> resolvedLibraries = driverPackage
                    .getSourceLibraryElements()
                    .parallelStream()
                    .map(e -> resolveLibrary(e, session))
                    .toList();
            if (resolvedLibraries.stream().anyMatch(l -> l.libraries().isEmpty())) return driverPackage;

            List<Library> libraries = resolvedLibraries
                    .stream()
                    .flatMap(l -> l.libraries().stream())
                    .collect(Collectors.toList());
            if (libraries.isEmpty()) return driverPackage;

            String id = driverPackage.getSourceId();
            int placeholderCount = countPlaceholders(id);
            id = getFormattedString(id, libraries, resolvedLibraries, placeholderCount, true);

            String name = driverPackage.getSourceName();
            placeholderCount = countPlaceholders(name);
            name = getFormattedString(name, libraries, resolvedLibraries, placeholderCount, false);

            driverPackage.setId(id);
            driverPackage.setName(name);
            driverPackage.setLibraries(libraries);
            driverPackage.setDetailsResolved(true);
            return driverPackage;
        } finally {
            driverPackage.setDetailsResolving(false);
        }
    }

    private boolean isLatestPackage(Element element) {
        return element.getChildren("library").stream()
                .anyMatch(e -> isLatestVersion(stringAttribute(e, "version")));
    }

    private List<DriverPackage> removeDuplicatePinnedPackages(List<DriverPackage> driverPackages) {
        Set<DriverVersionKey> latestDriverVersions = driverPackages.stream()
                .filter(DriverPackage::isLatest)
                .map(this::getDriverVersionKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (latestDriverVersions.isEmpty()) return driverPackages;

        return driverPackages.stream()
                .filter(p -> p.isLatest() || !latestDriverVersions.contains(getDriverVersionKey(p)))
                .toList();
    }

    @SneakyThrows
    private ResolvedLibrary resolveLibraryMetadata(Element element, DownloadSession session) {
        installThreadInterrupter(session);

        String groupId = stringAttribute(element, "group-id");
        String artifactId = stringAttribute(element, "artifact-id");
        String version = stringAttribute(element, "version");

        if (isLatestVersion(version)) {
            version = fetchLatestVersion(groupId, artifactId, session);
        } else {
            version = ensureVersion(groupId, artifactId, version);
        }

        Library library = createLibrary(element, groupId, artifactId, version);
        return new ResolvedLibrary(Collections.singletonList(library), getDriverRoleLibrary(library), getExtensionRoleLibrary(library));
    }

    private String fetchLatestVersion(String groupId, String artifactId, DownloadSession session) throws Exception {
        return session
                .versions(groupId, artifactId, () -> fetchAvailableVersions(groupId, artifactId, session))
                .stream()
                .max(Comparator.comparing(ComparableVersion::new))
                .orElseThrow(() -> new Exception("No versions found for " + groupId + ":" + artifactId));
    }

    private DriverVersionKey getDriverVersionKey(DriverPackage driverPackage) {
        return driverPackage.getLibraries().stream()
                .filter(l -> l.getRole() == DRIVER)
                .findFirst()
                .map(l -> new DriverVersionKey(driverPackage.getDatabaseType(), l.getGroupId(), l.getArtifactId(), l.getVersion()))
                .orElse(null);
    }

    private String getFormattedString(String s, List<Library> libraries, List<ResolvedLibrary> resolvedLibraries, int placeholderCount, boolean abridged) {
        if (placeholderCount == 1) {
            Library driverLibrary = getDriverLibrary(libraries, resolvedLibraries);
            String version = abridged ? shortenVersion(driverLibrary.getVersion()) : driverLibrary.getVersion();
            s = String.format(s, version);
        } else if (placeholderCount == 2) {
            Library driverLibrary = getDriverLibrary(libraries, resolvedLibraries);
            Library extensionLibrary = getExtensionLibrary(libraries, resolvedLibraries);
            String driverVersion = abridged ? shortenVersion(driverLibrary.getVersion()) : driverLibrary.getVersion();
            String extensionVersion = extensionLibrary.getVersion();
            s = String.format(s, driverVersion, extensionVersion);
        }
        return s;
    }

    private Library getDriverLibrary(List<Library> libraries, List<ResolvedLibrary> resolvedLibraries) {
        return resolvedLibraries.stream()
                .map(ResolvedLibrary::driverLibrary)
                .filter(Objects::nonNull)
                .findFirst()
                .or(() -> libraries.stream()
                        .filter(l -> l.getRole() == DRIVER)
                        .findFirst())
                .orElse(libraries.get(0));
    }

    private Library getExtensionLibrary(List<Library> libraries, List<ResolvedLibrary> resolvedLibraries) {
        return resolvedLibraries.stream()
                .map(ResolvedLibrary::extensionLibrary)
                .filter(Objects::nonNull)
                .findFirst()
                .or(() -> libraries.stream()
                        .filter(l -> l.getRole() == EXTENSION)
                        .findFirst())
                .orElse(getDriverLibrary(libraries, resolvedLibraries));
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
    private  ResolvedLibrary resolveLibrary(Element element, DownloadSession session) {
        installThreadInterrupter(session);

        String groupId = stringAttribute(element, "group-id");
        String artifactId = stringAttribute(element, "artifact-id");
        String version = stringAttribute(element, "version");
        String type = stringAttribute(element, "type");
        if (type == null) type = "jar";

        if (isLatestVersion(version)) {
            return createLatestLibrary(element, groupId, artifactId, type, session);
        }

        version = ensureVersion(groupId, artifactId, version);

        Library library = createLibrary(element, groupId, artifactId, version);
        return new ResolvedLibrary(Collections.singletonList(library), getDriverRoleLibrary(library), getExtensionRoleLibrary(library));
    }

    private ResolvedLibrary createLatestLibrary(Element element, String groupId, String artifactId, String type, DownloadSession session) throws Exception {
        List<String> versions = session.versions(groupId, artifactId, () -> fetchAvailableVersions(groupId, artifactId, session));
        List<String> recentVersions = versions.stream()
                .sorted(Comparator.comparing(ComparableVersion::new).reversed())
                .limit(MAX_LATEST_VERSION_ATTEMPTS)
                .toList();

        Throwable lastFailure = null;
        String latestVersion = recentVersions.get(0);
        for (String version : recentVersions) {
            Library library = createLibrary(element, groupId, artifactId, version);
            LibraryResolution resolution = resolveLibraryDependencies(library, type, session);
            if (resolution.isResolved()) {
                if (!Objects.equals(version, latestVersion)) {
                    log.warn("Resolved driver library '{}' using fallback version '{}' after latest version '{}' failed",
                            groupId + ":" + artifactId, version, latestVersion);
                }
                return new ResolvedLibrary(resolution.libraries(), getDriverRoleLibrary(library), getExtensionRoleLibrary(library));
            }
            lastFailure = resolution.failure();
        }

        String libraryId = groupId + ":" + artifactId + ":" + LATEST_VERSION;
        String message = lastFailure == null ? "No versions found" : lastFailure.getMessage();
        session.addErrorMessage(txt("msg.connection.error.FailedToDownloadLibrary", libraryId, message));
        return new ResolvedLibrary(Collections.emptyList(), null, null);
    }

    private LibraryResolution resolveLibraryDependencies(Library library, String type, DownloadSession session) {
        try {
            List<Library> libraries = session.libraries(library, type, () -> resolveDependencies(library, type, session));
            if (libraries.isEmpty()) {
                Throwable failure = new IllegalStateException("No dependencies resolved for " + library.getLibraryId());
                log.warn(failure.getMessage());
                return LibraryResolution.failed(failure);
            }

            copyChecksums(library, libraries);
            copyRole(library, libraries);
            return LibraryResolution.resolved(libraries);
        } catch (Throwable e) {
            Throwable failure = Exceptions.rootCauseOf(e);
            log.warn("Failed to resolve driver library '{}'", library.getLibraryId(), failure);
            return LibraryResolution.failed(failure);
        }
    }

    private Library createLibrary(Element element, String groupId, String artifactId, String version) {
        Library library = new Library(groupId, artifactId, version);
        library.setRole(enumAttribute(element, "role", LibraryRole.class));
        readChecksums(element, library);
        return library;
    }

    private Library getDriverRoleLibrary(Library library) {
        return library.getRole() == DRIVER ? library : null;
    }

    private Library getExtensionRoleLibrary(Library library) {
        return library.getRole() == EXTENSION ? library : null;
    }

    private void copyRole(Library source, List<Library> libraries) {
        LibraryRole role = source.getRole();
        if (role == null) return;

        libraries.stream()
                .filter(l -> Objects.equals(l.getGroupId(), source.getGroupId()))
                .filter(l -> Objects.equals(l.getArtifactId(), source.getArtifactId()))
                .filter(l -> Objects.equals(l.getVersion(), source.getVersion()))
                .forEach(l -> l.setRole(role));
    }

    private boolean isLatestVersion(String version) {
        return LATEST_VERSION.equalsIgnoreCase(version);
    }

    private List<Element> createDiscoveredDriverPackageElements(DatabaseType databaseType, List<Element> packageElements) {
        if (databaseType != DatabaseType.ORACLE) return Collections.emptyList();

        try {
            Set<String> declaredArtifacts = getDeclaredOracleProviderArtifacts(packageElements);
            return fetchOracleProviderArtifactIds()
                    .stream()
                    .filter(artifactId -> !declaredArtifacts.contains(artifactId))
                    .map(DriverPackageMetadataDownloader::createOracleProviderPackageElement)
                    .toList();
        } catch (Throwable e) {
            log.warn("Oracle JDBC provider artifact discovery failed", e);
            return Collections.emptyList();
        }
    }

    private Set<String> getDeclaredOracleProviderArtifacts(List<Element> packageElements) {
        return packageElements.stream()
                .flatMap(e -> e.getChildren("library").stream())
                .map(e -> stringAttribute(e, "artifact-id"))
                .filter(Objects::nonNull)
                .filter(DriverPackageMetadataDownloader::isOracleProviderArtifact)
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

    static Element createOracleProviderPackageElement(String artifactId) {
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

    static String toProviderName(String providerId) {
        return Pattern.compile("(^|-)([a-z])")
                .matcher(providerId)
                .replaceAll(match -> match.group(1).replace("-", " ") + match.group(2).toUpperCase());
    }

    private record LibraryResolution(List<Library> libraries, Throwable failure) {
        static LibraryResolution resolved(List<Library> libraries) {
            return new LibraryResolution(libraries, null);
        }

        static LibraryResolution failed(Throwable failure) {
            return new LibraryResolution(Collections.emptyList(), failure);
        }

        boolean isResolved() {
            return !libraries.isEmpty();
        }
    }

    private record ResolvedLibrary(List<Library> libraries, Library driverLibrary, Library extensionLibrary) {}

    private record DriverVersionKey(DatabaseType databaseType, String groupId, String artifactId, String version) {}

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

    private  String ensureVersion(String groupId, String artifactId, String currentVersion) throws Exception{
        if (currentVersion != null && isValidVersion(currentVersion)) return currentVersion;

        if (currentVersion == null) {
            throw new Exception("Missing version declaration for " + groupId + ":" + artifactId);
        }

        throw new Exception("Unsupported version declaration: " + currentVersion);
    }

    @SneakyThrows
    private  List<String> fetchAvailableVersions(String groupId, String artifactId, DownloadSession session) {
        // URL for Maven metadata
        String url = MavenRepositories.CENTRAL_URL + "/" + groupId.replace('.', '/') + "/" + artifactId + "/maven-metadata.xml";
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
