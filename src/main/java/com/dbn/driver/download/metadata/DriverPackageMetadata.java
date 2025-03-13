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

package com.dbn.driver.download.metadata;

import com.dbn.common.checksum.Checksum;
import com.dbn.common.checksum.ChecksumType;
import com.dbn.common.message.AsyncMessageCollector;
import com.dbn.common.util.XmlContents;
import com.dbn.connection.DatabaseType;
import com.dbn.driver.download.DownloadSession;
import com.dbn.driver.download.DownloadStatus;
import com.dbn.driver.download.DriverDownloadManager;
import com.dbn.driver.download.DriverMetadataDownloader;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.dbn.common.util.Lists.convertParallel;
import static com.dbn.driver.download.DriverDownloadManager.getDriverPackageChecksumsLocation;
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
public class DriverPackageMetadata {
    @Getter
    private List<DriverPackage> driverPackages = new ArrayList<>();
    @Getter
    private List<DriverPackage> cachedDriverPackages = new ArrayList<>();

    @SneakyThrows
    public void ensureDriverPackages(DownloadSession session) {
        if(!driverPackages.isEmpty()) return;
        Element element = XmlContents.fileToElement(getClass(), "driver-packages.xml");
        List<Element> packageElements = element.getChildren("driver-package");
        session.withDownloadSize(packageElements.size());


        Function<Element, DriverPackage> driverPackageFunction = e->
                new DriverMetadataDownloader().createDriverPackage(e, session);

        List<DriverPackage> newlyLoadedPackages = convertParallel(packageElements, driverPackageFunction);
        for (DriverPackage newPkg : newlyLoadedPackages) {
            cachedDriverPackages.stream()
                    .filter(cachedPkg -> cachedPkg.getId().equals(newPkg.getId()))
                    .findFirst()
                    .ifPresent(cachedPkg -> {
                        newPkg.setPath(cachedPkg.getPath());
                    });
        }
        for (DriverPackage cachedPkg : cachedDriverPackages) {
            boolean exists = newlyLoadedPackages.stream()
                    .anyMatch(dp -> dp.getId().equals(cachedPkg.getId()));
            if (!exists) {
                newlyLoadedPackages.add(cachedPkg);
            }
        }
        // Finally, wrap in an unmodifiableList so no one can add/remove packages
        driverPackages = unmodifiableList(newlyLoadedPackages);
    }

    public List<DriverPackage> loadAllDriverPackages(DownloadSession session, DatabaseType databaseType) {
        ensureDriverPackages(session);
        return driverPackages.stream().filter(dp -> dp.getDatabaseType() == databaseType || databaseType == DatabaseType.GENERIC)
                .filter(dp -> !dp.isOld())
                .collect(Collectors.toList());
    }

    public List<DriverPackage> getDownloadedDriverPackage(AsyncMessageCollector messages, DatabaseType databaseType) {
        try {
                return verifyDriverPackages(driverPackages.isEmpty()?cachedDriverPackages:driverPackages).stream()
                        .filter(dp -> dp.getDatabaseType() == databaseType || databaseType == DatabaseType.GENERIC)
                        .filter(driverPackage -> DriverDownloadManager.getInstance().isPackageDownloaded(driverPackage.getId(), driverPackages.isEmpty()))
                        .collect(Collectors.toList());

        } catch (Exception e) {
            messages.addErrorMessage(e.getMessage());
            return new ArrayList<>();
        }
    }

    @NotNull
    public DriverPackage getDriverPackage(String packageId) {
        Optional<DriverPackage> driverPackage = driverPackages.stream()
                .filter(p -> p.getId().equals(packageId))
                .findFirst();

        if (driverPackage.isPresent()) {
            return driverPackage.get();
        }

        DriverPackage newPackage = new DriverPackage(packageId);
        if (driverPackages == null) {
            driverPackages = new ArrayList<>();
        }
        driverPackages.add(newPackage);

        return newPackage;
    }

    @NotNull
    public DriverPackage getCachedDriverPackage(String packageId) {
        Optional<DriverPackage> driverPackage = cachedDriverPackages.stream()
                .filter(p -> p.getId().equals(packageId))
                .findFirst();

        if (driverPackage.isPresent()) {
            return driverPackage.get();
        }

        DriverPackage newPackage = new DriverPackage(packageId);
        newPackage.setOld(true);
        if (cachedDriverPackages == null) {
            cachedDriverPackages = new ArrayList<>();
        }
        cachedDriverPackages.add(newPackage);

        return newPackage;
    }

    /**
     * Verifies the checksums for the given list of DriverPackage objects.
     * Cleans up packages (via DriverDownloadManager) if checksums fail.
     *
     * @param driverPackages The list of DriverPackage objects to verify.
     * @return The same list of DriverPackage objects (for convenience).
     * @throws Exception If any I/O or related errors occur during verification.
     */
    private List<DriverPackage> verifyDriverPackages(List<DriverPackage> driverPackages) throws Exception {
        // Check if there's any package status to process
        if (!DriverDownloadManager.getInstance().getPackagesStatus().isEmpty()) {
            for (DriverPackage driverPackage : driverPackages) {
                String packageId = driverPackage.getId();
                // If path is null, nothing to validate here
                if (driverPackage.getPath() == null) {
                    continue;
                }

                File packageDir = new File(driverPackage.getPath());
                File checksumFile = new File(getDriverPackageChecksumsLocation(), packageId + ".txt");

                // If no checksum file exists, all libraries are set to NEW
                if (!checksumFile.exists()) {
                    for (Library library : driverPackage.getLibraries()) {
                        String jarId = library.getArtifactId() + "-" + library.getVersion();
                        DriverDownloadManager.getInstance()
                                .updateJarDownloadStatus(packageId, jarId, DownloadStatus.NEW);
                    }
                    continue;
                }

                // Verify each line (artifactId-version, expectedChecksum)
                try (BufferedReader reader = new BufferedReader(new FileReader(checksumFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(" ");
                        if (parts.length != 2) {
                            continue;
                        }

                        String artifactIdVersion = parts[0]; // e.g., artifactId-version
                        String expectedChecksum = parts[1];
                        File jarFile = new File(packageDir, artifactIdVersion + ".jar");

                        if (!jarFile.exists()
                                || !verifyChecksum(jarFile, expectedChecksum, artifactIdVersion)) {
                            // If something is wrong, clean up and move on
                            DriverDownloadManager.getInstance().cleanupPackage(packageId);
                            break;
                        }
                    }
                }
            }
        }
        return driverPackages;
    }

    private boolean verifyChecksum(File outputFile, String expectedChecksum, String artifactIdAndVersion) {
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
}