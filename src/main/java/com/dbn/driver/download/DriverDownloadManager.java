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
import com.dbn.common.message.AsyncMessageCollector;
import com.dbn.driver.download.metadata.DriverPackage;
import com.dbn.driver.download.metadata.DriverPackageMetadata;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.component.Components.applicationService;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.stringAttribute;
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

    private final DriverPackageMetadata driverPackageMetadata = new DriverPackageMetadata();

    public static DriverDownloadManager getInstance() {
        return applicationService(DriverDownloadManager.class);
    }

    public static DriverPackageMetadata getDriverPackageMetadata() {
        return getInstance().driverPackageMetadata;
    }

    public DriverDownloadManager() {
        super(COMPONENT_NAME);
    }

    /**
     * Registers the download status of an individual JAR in a driver package, along with a timestamp.
     *
     * @param packageId Unique ID of the driver package
     * @param jarId     Unique ID of the JAR (e.g., "artifactId-version")
     */
    public DriverPackageStatus.LibraryStatus getJarDownloadStatus(String packageId, String jarId) {
        return getPackageStatus(packageId)
                .getLibraryStatus(jarId);
    }

    public void updateJarDownloadStatus(String packageId, String jarId, DownloadStatus status) {
        log.info("Download status for package {} JAR {}: {}", packageId, jarId, status);

        getPackageStatus(packageId)
                .getLibraryStatus(jarId)
                .setDownloadStatus(status);
    }

    private DriverPackageStatus getPackageStatus(String packageId) {
        AsyncMessageCollector messages = new AsyncMessageCollector();
        return packageDownloadStatuses
                .computeIfAbsent(packageId, k -> {
                    try {
                        return createPackageStatus(packageId);
                    } catch (Exception e) {
                        messages.addErrorMessage(e.getMessage());
                        throw new RuntimeException(e.getMessage(), e);
                    }
                });
    }

    public Collection<DriverPackageStatus> getPackagesStatus() {
        return packageDownloadStatuses.values();
    }

    private DriverPackageStatus createPackageStatus(String packageId) {
        return new DriverPackageStatus(packageId);
    }

    /**
     * Checks if all JARs in a driver package have been fully downloaded and verified.
     *
     * @param packageId Unique ID of the driver package
     * @return True if all JARs in the package are verified, false otherwise
     */
    public boolean isPackageDownloaded(String packageId, boolean isCached) {
        DriverPackageStatus jarStatuses = getPackageStatus(packageId);
        if (jarStatuses == null) {
            return false;
        }
        DriverPackage dp = isCached?driverPackageMetadata.getCachedDriverPackage(packageId):driverPackageMetadata.getDriverPackage(packageId);
        return jarStatuses.isComplete(dp.getLibraries().size());
    }

    public void cleanupPackage(String packageId) {
        getPackageStatus(packageId).getLibraryStatuses().forEach(libraryStatus->libraryStatus.setDownloadStatus(DownloadStatus.NEW));
   }

    @Override
    public Element getComponentState() {
        Element element = new Element("state");
        Element downloadsElement = newElement(element, "package-download-statuses");
        Element metadataElement = newElement(element, "package-metadata");
        for (Map.Entry<String, DriverPackageStatus> packageEntry : packageDownloadStatuses.entrySet()) {
            Element packageElement = newElement(downloadsElement, "package");
            packageEntry.getValue().writeState(packageElement);
        }
        for (DriverPackage driverPackage : driverPackageMetadata.getDriverPackages()) {
            if(isPackageDownloaded(driverPackage.getId(), false)){
                Element packageElement = newElement(metadataElement, "package");
                driverPackage.writeState(packageElement);
            }
        }
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        Element downloadsElement = element.getChild("package-download-statuses");
        if (downloadsElement != null) {
            for (Element packageElement : downloadsElement.getChildren("package")) {
                String packageId = packageElement.getAttributeValue("id");
                DriverPackageStatus jarStatuses = getPackageStatus(packageId);
                jarStatuses.readState(packageElement);
            }
        }
        Element metadataElement = element.getChild("package-metadata");
        if (metadataElement != null) {
            for (Element packageElement : metadataElement.getChildren("package")) {
                DriverPackage driverPackage = driverPackageMetadata.getCachedDriverPackage(stringAttribute(packageElement, "id"));
                driverPackage.readState(packageElement);
            }
        }
    }


}