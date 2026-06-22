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

import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.TimeUtil;
import com.dbn.connection.DatabaseType;
import com.dbn.driver.download.DownloadSession;
import com.dbn.driver.download.DownloadStatus;
import com.dbn.driver.download.DriverDownloadManager;
import com.dbn.driver.download.PackageChecksumData;
import com.intellij.openapi.progress.ProgressIndicator;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.stringAttribute;

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
@Setter
public class DriverPackageMetadata implements PersistentStateElement {
    private final Map<DatabaseType, Long> lastRefresh = new ConcurrentHashMap<>();
    private final Map<String, DriverPackage> driverPackages = new ConcurrentHashMap<>();

    @SneakyThrows
    public synchronized void refreshDriverPackages(DatabaseType databaseType) {
        if (!isOutdated(databaseType)) return;

        ProgressIndicator indicator = ProgressMonitor.getProgressIndicator();
        DownloadSession session = new DownloadSession(indicator);

        DriverPackageMetadataDownloader downloader = new DriverPackageMetadataDownloader();
        Map<String, DriverPackage> driverPackages = downloader.createDriverPackages(session, databaseType);
        Set<String> packageIds = driverPackages.keySet();

        // Mark packages obsolete only for the refreshed database type.
        this.driverPackages.values().stream()
                .filter(p -> p.matches(databaseType))
                .forEach(p -> p.setObsolete(!packageIds.contains(p.getId())));
        this.driverPackages.putAll(driverPackages);

        verifyDriverPackages();
        lastRefresh.put(databaseType, System.currentTimeMillis());
    }

    public boolean isOutdated(DatabaseType databaseType) {
        long refreshTime = lastRefresh.getOrDefault(databaseType, 0L);
        return TimeUtil.isOlderThan(refreshTime, 1, TimeUnit.HOURS);
    }

    public List<DriverPackage> getDriverPackages(DatabaseType databaseType, Predicate<DriverPackage> predicate) {
        refreshDriverPackages(databaseType);
        return driverPackages.values().stream()
                .sorted()
                .filter(p -> p.matches(databaseType))
                .filter(predicate)
                .collect(Collectors.toList());
    }

    public synchronized DriverPackage resolveDriverPackageDetails(DriverPackage driverPackage, DownloadSession session) {
        if (driverPackage == null) return null;

        String originalId = driverPackage.getId();
        DriverPackageMetadataDownloader downloader = new DriverPackageMetadataDownloader();
        DriverPackage resolvedPackage = downloader.resolveDriverPackageDetails(driverPackage, session);
        if (resolvedPackage != null && !originalId.equals(resolvedPackage.getId())) {
            driverPackages.remove(originalId);
            driverPackages.put(resolvedPackage.getId(), resolvedPackage);
        }
        return resolvedPackage;
    }


    @NotNull
    public DriverPackage ensureDriverPackage(String packageId) {
        return driverPackages.computeIfAbsent(packageId, id -> new DriverPackage(id));
    }

    @Nullable
    public DriverPackage getDriverPackage(String packageId) {
        return driverPackages.computeIfAbsent(packageId, id -> new DriverPackage(id));
    }

    /**
     * Verifies the checksums for the given list of DriverPackage objects.
     * Cleans up packages (via DriverDownloadManager) if checksums fail.
     *
     * @throws Exception If any I/O or related errors occur during verification.
     */
    private void verifyDriverPackages() throws Exception {
        // Check if there's any package status to process
        DriverDownloadManager downloadManager = DriverDownloadManager.getInstance();
        if (downloadManager.getPackagesStatus().isEmpty()) return;

        for (DriverPackage driverPackage : driverPackages.values()) {
            verifyDriverPackage(driverPackage);
        }
    }

    private void verifyDriverPackage(DriverPackage driverPackage) {
        if (!driverPackage.isDetailsAvailable()) return;

        DriverDownloadManager downloadManager = DriverDownloadManager.getInstance();
        String packageId = driverPackage.getId();
        String downloadPath = downloadManager.getDownloadPath(packageId);

        // If path is null, nothing to validate here
        if (downloadPath == null) return;

        File packageDir = new File(downloadPath);
        PackageChecksumData checksumData = downloadManager.getChecksumData(packageId);
        List<String> libraryIds = driverPackage.getLibraryIds();
        downloadManager.reconcilePackageStatus(driverPackage);

        // If no checksum file exists, all libraries are set to NEW
        if (checksumData.fileExists()) {
            checksumData.readChecksums();
            boolean checksumsValid = checksumData.verifyChecksums(packageDir, libraryIds);
            if (!checksumsValid) {
                downloadManager.cleanupPackage(packageId);
            }
        } else {
            for (String libraryId : libraryIds) {
                downloadManager.setDownloadStatus(packageId, libraryId, DownloadStatus.NEW);
            }

        }
    }

    @Override
    public void readState(Element element) {
        for (Element packageElement : element.getChildren("package")) {
            String packageId = stringAttribute(packageElement, "id");
            DriverPackage driverPackage = ensureDriverPackage(packageId);
            driverPackage.readState(packageElement);
        }
    }

    @Override
    public void writeState(Element element) {
        for (DriverPackage driverPackage : driverPackages.values()) {
            Element packageElement = newElement(element, "package");
            driverPackage.writeState(packageElement);
        }
    }
}
