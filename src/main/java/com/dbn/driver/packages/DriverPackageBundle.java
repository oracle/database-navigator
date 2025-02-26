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
import com.dbn.common.message.AsyncMessageCollector;
import com.dbn.common.util.XmlContents;
import com.dbn.connection.DatabaseType;
import com.intellij.openapi.progress.ProgressIndicator;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private DriverPackageBundle(ProgressIndicator indicator, AsyncMessageCollector messages) {
        Element element = XmlContents.fileToElement(getClass(), "driver-packages.xml");
        List<Element> packageElements = element.getChildren("driver-package");
        Function<Element, DriverPackage> driverPackageFunction = e->
                new DriverMetaDataDownloader().createDriverPackage(e, indicator, messages,(float) 1 /packageElements.size());
        driverPackages = unmodifiableList(convertParallel(packageElements, driverPackageFunction));
    }

    public synchronized static void initialize(ProgressIndicator indicator, AsyncMessageCollector messages) {
        if (INSTANCE == null) {
            INSTANCE = new DriverPackageBundle(indicator, messages);
        }
    }
    private static final String DRIVER_PACKAGES_PATH = getPluginDeploymentRoot().getPath()+"/driver-packages/checksums";

    public static List<DriverPackage> driverPackages(AsyncMessageCollector messages) {
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
                    messages.addErrorMessage("Error reading checksum file for package: " + packageId);
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

    public static List<DriverPackage> driverPackages(DatabaseType databaseType, ProgressIndicator indicator, AsyncMessageCollector messages) {
        initialize(indicator, messages);
        return driverPackages(messages).stream().filter(dp -> dp.getDatabaseType() == databaseType || dp.getDatabaseType() == DatabaseType.GENERIC).collect(Collectors.toList());
    }

    public static List<DriverPackage> getDownloadedDriverPackage(AsyncMessageCollector messages) {
        if (INSTANCE == null) return new ArrayList<>();
        try {
            return driverPackages(messages).stream()
                    .filter(driverPackage -> DriverDownloadManager.getInstance().isPackageDownloaded(driverPackage.getId()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @NotNull
    public static DriverPackage getDriverPackage(String packageId, AsyncMessageCollector messages) {
        Optional<DriverPackage> driverPackage = driverPackages(messages).stream().filter(p -> p.getId().equals(packageId)).findFirst();
        if(driverPackage.isPresent()){
            return driverPackage.get();
        } else {
            throw new IllegalArgumentException("Package with id " + packageId + " not found");
        }
    }
}