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

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.ApplicationComponentBase;
import com.dbn.common.component.PersistentState;
import com.dbn.connection.DatabaseType;
import com.dbn.driver.download.metadata.DriverPackage;
import com.dbn.driver.download.metadata.DriverPackageMetadata;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.component.Components.applicationService;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.util.Files.getPluginDeploymentRoot;
import static com.dbn.driver.download.DownloadStatus.NEW;
import static com.dbn.driver.download.DriverDownloadManager.COMPONENT_NAME;

/**
 * Download Manager for tracking the state of driver package downloads.
 * <p>
 * Features:
 * <ol>
 * <li>Tracks download status and timestamp of each JAR within a driver package by package ID and JAR identifier</li>
 * <li>Stores if individual JARs in a package were successfully downloaded and verified</li>
 * <li>Supports cleanup of download records if verification fails for any JAR in a package</li>
 * </ol>
 */
@Slf4j
@State(
        name = COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DriverDownloadManager extends ApplicationComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Application.DriverDownloadManager";

    private final Map<String, DriverPackageStatus> packageDownloadStatuses = new ConcurrentHashMap<>();
    private final Map<String, PackageChecksumData> packageChecksums = new ConcurrentHashMap<>();

    private final DriverPackageMetadata driverPackageMetadata = new DriverPackageMetadata();

    public static DriverDownloadManager getInstance() {
        return applicationService(DriverDownloadManager.class);
    }

    public DriverDownloadManager() {
        super(COMPONENT_NAME);
    }

    @NonNls
    public static String getDriverPackagesLocation() {
        return getPluginDeploymentRoot().getPath() + File.separator + "driver-packages";
    }

    @NonNls
    public static String getDriverPackageLocation(String packageId) {
        return getDriverPackagesLocation() + File.separator + packageId;
    }

    @NonNls
    public static String getDriverPackageChecksumsLocation() {
        return getDriverPackagesLocation() + File.separator + "checksums";
    }

    /**
     * Registers the download status of an individual JAR in a driver package, along with a timestamp.
     *
     * @param packageId Unique ID of the driver package
     * @param jarId     Unique ID of the JAR (e.g., "artifactId-version")
     */
    public DownloadStatus getDownloadStatus(String packageId, String jarId) {
        DriverPackageStatus packageStatus = getPackageStatus(packageId);
        if (packageStatus == null) return NEW;

        DriverPackageStatus.LibraryStatus libraryStatus = packageStatus.getLibraryStatus(jarId);
        if (libraryStatus == null) return NEW;

        return libraryStatus.getDownloadStatus();
    }

    public void setDownloadStatus(String packageId, String libraryId, DownloadStatus status) {
        log.info("Download status for package {} JAR {}: {}", packageId, libraryId, status);

        ensurePackageStatus(packageId)
                .ensureLibraryStatus(libraryId)
                .setDownloadStatus(status);
    }

    @Nullable
    private DriverPackageStatus getPackageStatus(String packageId) {
        return packageDownloadStatuses.get(packageId);
    }

    @NotNull
    private DriverPackageStatus ensurePackageStatus(String packageId) {
        return packageDownloadStatuses.computeIfAbsent(packageId, k -> new DriverPackageStatus(packageId));
    }

    @Nullable
    public String getDownloadPath(String packageId) {
        DriverPackageStatus packageStatus = packageDownloadStatuses.get(packageId);
        return packageStatus == null ? null : packageStatus.getDownloadPath() ;
    }

    public void setDownloadPath(String packageId, String path) {
        DriverPackageStatus packageStatus = ensurePackageStatus(packageId);
        packageStatus.setDownloadPath(path);
    }

    public Collection<DriverPackageStatus> getPackagesStatus() {
        return packageDownloadStatuses.values();
    }

    @NotNull
    public PackageChecksumData getChecksumData(String packageId) {
        return packageChecksums.computeIfAbsent(packageId, p -> new PackageChecksumData(p));
    }

    public void setChecksumData(String packageId, PackageChecksumData checksumData) {
        packageChecksums.put(packageId, checksumData);
    }

    /**
     * Checks if all JARs in a driver package have been fully downloaded and verified.
     *
     * @param packageId Unique ID of the driver package
     * @return True if all JARs in the package are verified, false otherwise
     */

    public boolean isPackageDownloaded(String packageId) {
        DriverPackage driverPackage = driverPackageMetadata.getDriverPackage(packageId);
        return isPackageDownloaded(driverPackage);
    }

    public boolean isPackageDownloaded(DriverPackage driverPackage) {
        if (driverPackage == null) return false;

        DriverPackageStatus status = getPackageStatus(driverPackage.getId());
        if (status == null) return false;

        return status.isComplete(driverPackage.getLibraries().size());
    }

    public void cleanupPackage(String packageId) {
        DriverPackageStatus packageStatus = getPackageStatus(packageId);
        if (packageStatus == null) return;

        packageStatus.getLibraryStatuses().forEach(libraryStatus->libraryStatus.setDownloadStatus(NEW));
   }

    public List<DriverPackage> getDownloadedDriverPackages(DatabaseType databaseType) {
        return driverPackageMetadata.getDriverPackages(p -> p.matches(databaseType) && isPackageDownloaded(p));
    }

    public List<DriverPackage> getDriverPackages(DatabaseType databaseType) {
        return driverPackageMetadata.getDriverPackages(p -> p.matches(databaseType) && (!p.isObsolete() || isPackageDownloaded(p)));
    }

    @Override
    public Element getComponentState() {
        Element element = new Element("state");
        Element downloadsElement = newElement(element, "package-downloads");
        Element metadataElement = newElement(element, "package-metadata");
        for (DriverPackageStatus packageStatus : packageDownloadStatuses.values()) {
            Element packageElement = newElement(downloadsElement, "package");
            packageStatus.writeState(packageElement);
        }

        driverPackageMetadata.writeState(metadataElement);
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        Element downloadsElement = element.getChild("package-downloads");
        if (downloadsElement != null) {
            for (Element packageElement : downloadsElement.getChildren("package")) {
                DriverPackageStatus packageStatus = new DriverPackageStatus();
                packageStatus.readState(packageElement);
                packageDownloadStatuses.put(packageStatus.getPackageId(), packageStatus);
            }
        }
        Element metadataElement = element.getChild("package-metadata");
        driverPackageMetadata.readState(metadataElement);
    }

}