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
import com.dbn.common.routine.Consumer;
import com.dbn.common.thread.ProgressInfo;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.config.ConnectionBundleSettings;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.provider.CloudConfigProviderFamily;
import com.dbn.driver.DriverSource;
import com.dbn.driver.dashboard.DriverLibraryDashboardDialog;
import com.dbn.driver.dashboard.DriverLibraryDashboardOptions;
import com.dbn.driver.download.metadata.DriverPackage;
import com.dbn.driver.download.metadata.DriverPackageMetadata;
import com.dbn.driver.download.metadata.Library;
import com.dbn.driver.download.ui.DriverDownloadDialog;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.component.Components.applicationService;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.util.Conditional.when;
import static com.dbn.common.util.Files.getPluginDeploymentRoot;
import static com.dbn.common.util.Messages.showErrorDialog;
import static com.dbn.driver.download.DownloadStatus.DONE;
import static com.dbn.driver.download.DownloadStatus.NEW;
import static com.dbn.driver.download.DriverDownloadManager.COMPONENT_NAME;
import static com.dbn.nls.NlsResources.txt;

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

    private final Map<String, DriverPackageDownloadState> downloadStates = new ConcurrentHashMap<>();
    private final Map<DatabaseType, DriverPackageMetadataRefresh> metadataRefreshes = new ConcurrentHashMap<>();

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
        DriverPackageDownloadState downloadState = getPackageDownloadState(packageId);
        if (downloadState == null) return NEW;

        DriverPackageDownloadInfo.LibraryStatus libraryStatus = downloadState.getDownloadInfo().getLibraryStatus(jarId);
        if (libraryStatus == null) return NEW;

        return libraryStatus.getDownloadStatus();
    }

    public void setDownloadStatus(String packageId, String libraryId, DownloadStatus status) {
        log.info("Download status for package {} JAR {}: {}", packageId, libraryId, status);

        DriverPackageDownloadInfo.LibraryStatus libraryStatus = ensurePackageDownloadState(packageId).getDownloadInfo().ensureLibraryStatus(libraryId);
        libraryStatus.setDownloadStatus(status);
        if (status == DONE) {
            libraryStatus.setDownloadTimestamp(System.currentTimeMillis());
        }
    }

    @Nullable
    private DriverPackageDownloadState getPackageDownloadState(String packageId) {
        return downloadStates.get(packageId);
    }

    @NotNull
    private DriverPackageDownloadState ensurePackageDownloadState(String packageId) {
        return downloadStates.computeIfAbsent(packageId, DriverPackageDownloadState::new);
    }

    @Nullable
    public String getDownloadPath(String packageId) {
        DriverPackageDownloadState downloadState = getPackageDownloadState(packageId);
        return downloadState == null ? null : downloadState.getDownloadInfo().getDownloadPath() ;
    }

    public void setDownloadPath(String packageId, String path) {
        DriverPackageDownloadState downloadState = ensurePackageDownloadState(packageId);
        DriverPackageDownloadInfo downloadInfo = downloadState.getDownloadInfo();
        if (!Objects.equals(downloadInfo.getDownloadPath(), path)) {
            resetPackageDownloadStatuses(packageId);
        }
        downloadInfo.setDownloadPath(path);
    }

    public Collection<DriverPackageDownloadState> getPackageDownloadStates() {
        return downloadStates.values().stream()
                .filter(DriverPackageDownloadState::hasPersistedDownloadInfo)
                .toList();
    }

    @NotNull
    public PackageChecksumData getChecksumData(String packageId) {
        return ensurePackageDownloadState(packageId).getChecksumData();
    }

    public void setChecksumData(String packageId, PackageChecksumData checksumData) {
        ensurePackageDownloadState(packageId).setChecksumData(checksumData);
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
        if (!driverPackage.isDetailsAvailable()) return false;

        DriverPackageDownloadState downloadState = getPackageDownloadState(driverPackage.getId());
        if (downloadState == null) return false;

        List<String> libraryIds = driverPackage.getLibraryIds();
        reconcilePackageDownloadState(driverPackage);
        return downloadState.getDownloadInfo().isComplete(libraryIds) && hasVerifiedChecksumManifest(driverPackage, downloadState);
    }

    public void reconcilePackageDownloadState(DriverPackage driverPackage) {
        DriverPackageDownloadState downloadState = getPackageDownloadState(driverPackage.getId());
        if (downloadState == null) return;

        List<String> libraryIds = driverPackage.getLibraryIds();
        downloadState.getDownloadInfo().retainLibraryStatuses(libraryIds);

        String downloadPath = downloadState.getDownloadInfo().getDownloadPath();
        if (downloadPath == null) return;

        PackageChecksumData checksumData = getChecksumData(driverPackage.getId());
        if (!checksumData.fileExists()) return;

        checksumData.readChecksums();
        File packageDir = new File(downloadPath);
        if (checksumData.retainChecksums(packageDir, libraryIds)) {
            checksumData.writeChecksums();
        }
    }

    private boolean hasVerifiedChecksumManifest(DriverPackage driverPackage, DriverPackageDownloadState downloadState) {
        String downloadPath = downloadState.getDownloadInfo().getDownloadPath();
        if (downloadPath == null) return false;

        PackageChecksumData checksumData = getChecksumData(driverPackage.getId());
        if (!checksumData.fileExists()) return false;

        checksumData.readChecksums();
        File packageDir = new File(downloadPath);
        List<String> libraryIds = driverPackage.getLibraryIds();
        return checksumData.verifyChecksums(packageDir, libraryIds);
    }

    public void resetPackageDownloadStatuses(String packageId) {
        DriverPackageDownloadState downloadState = getPackageDownloadState(packageId);
        if (downloadState == null) return;

        downloadState.getDownloadInfo().getLibraryStatuses().forEach(libraryStatus->libraryStatus.setDownloadStatus(NEW));
   }

    public List<DriverPackage> getDownloadedDriverPackages(DatabaseType databaseType) {
        return getDownloadedDriverPackages(databaseType, null);
    }

    public List<DriverPackage> getDownloadedDriverPackages(
            DatabaseType databaseType,
            @Nullable CloudConfigProviderFamily providerFamily) {
        return driverPackageMetadata.getDriverPackages(databaseType, providerFamily, p -> isPackageDownloaded(p));
    }

    public List<DriverPackage> getDriverPackages(DatabaseType databaseType) {
        return getDriverPackages(databaseType, null);
    }

    public List<DriverPackage> getDriverPackages(
            DatabaseType databaseType,
            @Nullable CloudConfigProviderFamily providerFamily) {
        return driverPackageMetadata.getDriverPackages(databaseType, providerFamily, p -> !p.isObsolete() || isPackageDownloaded(p));
    }

    public List<DriverPackage> getCachedDriverPackages(
            DatabaseType databaseType,
            @Nullable CloudConfigProviderFamily providerFamily) {
        return driverPackageMetadata.getCachedDriverPackages(databaseType, providerFamily, p -> !p.isObsolete() || isPackageDownloaded(p));
    }

    public void refreshDriverPackageMetadata(
            DatabaseType databaseType,
            @Nullable CloudConfigProviderFamily providerFamily,
            DriverPackageMetadata.RefreshMode refreshMode) {
        driverPackageMetadata.refreshDriverPackages(databaseType, providerFamily, refreshMode);
    }

    /**
     * Returns a shared metadata and package-detail refresh for this database type.
     * It refreshes all provider families so settings and menu contexts can observe the same process.
     */
    public DriverPackageMetadataRefresh refreshDriverPackageInventory(
            Project project,
            DatabaseType databaseType,
            boolean forceReload) {
        List<CloudConfigProviderFamily> providerFamilies = List.of(CloudConfigProviderFamily.values());
        return metadataRefreshes.compute(databaseType, (ignored, current) -> {
            if (current != null && current.isRunning()) return current;
            boolean metadataOutdated = providerFamilies.stream()
                    .anyMatch(family -> isDriverPackageMetadataOutdated(databaseType, family));
            if (current != null && !forceReload) {
                ProgressInfo lastProgress = current.getProgress();
                boolean failed = !lastProgress.errors().isEmpty() ||
                        !lastProgress.groupedErrors().isEmpty() || lastProgress.canceled();
                if (failed || !metadataOutdated) return current;
            }

            DriverPackageMetadataRefresh refresh = new DriverPackageMetadataRefresh(databaseType, providerFamilies, forceReload);
            refresh.start(project);
            return refresh;
        });
    }

    public boolean isDriverPackageMetadataOutdated(DatabaseType databaseType) {
        return isDriverPackageMetadataOutdated(databaseType, null);
    }

    public boolean isDriverPackageMetadataOutdated(
            DatabaseType databaseType,
            @Nullable CloudConfigProviderFamily providerFamily) {
        return driverPackageMetadata.isOutdated(databaseType, providerFamily);
    }

    public DriverPackage resolveDriverPackageDetails(DriverPackage driverPackage, DownloadSession session) {
        return driverPackageMetadata.resolveDriverPackageDetails(driverPackage, session);
    }

    public void openLibraryDashboard(Project project, DriverLibraryDashboardOptions options) {
        Dialogs.show(() -> new DriverLibraryDashboardDialog(project, options));
    }

    public boolean beginPackageDownload(String packageId) {
        return ensurePackageDownloadState(packageId).beginPackageDownload();
    }

    public void endPackageDownload(String packageId) {
        DriverPackageDownloadState downloadState = getPackageDownloadState(packageId);
        if (downloadState != null) downloadState.endPackageDownload();
    }

    public boolean isPackageDownloading(String packageId) {
        DriverPackageDownloadState downloadState = getPackageDownloadState(packageId);
        return downloadState != null && downloadState.isPackageDownloading();
    }

    /** Returns whether this package has a complete tracked download record, even if a prior cleanup partially removed files. */
    public boolean isPackageTrackedForDeletion(DriverPackage driverPackage) {
        if (driverPackage == null || driverPackage.getLibraryIds().isEmpty()) return false;
        DriverPackageDownloadState downloadState = getPackageDownloadState(driverPackage.getId());
        return downloadState != null &&
                downloadState.getDownloadInfo().getDownloadPath() != null &&
                downloadState.getDownloadInfo().isComplete(driverPackage.getLibraryIds());
    }

    public boolean isPackageReferenced(Project currentProject, DriverPackage driverPackage) {
        DriverPackageDownloadState downloadState = getPackageDownloadState(driverPackage.getId());
        if (downloadState == null || downloadState.getDownloadInfo().getDownloadPath() == null) return false;

        Set<Project> projects = new HashSet<>(List.of(ProjectManager.getInstance().getOpenProjects()));
        if (currentProject != null && !currentProject.isDisposed()) projects.add(currentProject);

        for (Project project : projects) {
            if (project.isDisposed()) continue;
            for (ConnectionSettings connection : ConnectionBundleSettings.getInstance(project).getConnections()) {
                if (connection.getDatabaseSettings().getDriverSource() != DriverSource.EXTERNAL) continue;
                File libraryFile = connection.getDatabaseSettings().getDriverLibraryFile();
                if (libraryFile == null) continue;
                if (isPackagePathReferenced(libraryFile.getPath(), downloadState.getDownloadInfo().getDownloadPath())) return true;
            }
        }
        return false;
    }

    public PackageDeletionResult deleteDownloadedPackage(Project currentProject, DriverPackage driverPackage) {
        String packageId = driverPackage.getId();
        if (isPackageDownloading(packageId)) return new PackageDeletionResult(false, DeletionBlock.DOWNLOADING, List.of());
        if (isPackageReferenced(currentProject, driverPackage)) return new PackageDeletionResult(false, DeletionBlock.IN_USE, List.of());
        if (!isPackageTrackedForDeletion(driverPackage)) return new PackageDeletionResult(false, DeletionBlock.NOT_DOWNLOADED, List.of());

        DriverPackageDownloadState downloadState = getPackageDownloadState(packageId);
        File packageDirectory = new File(downloadState.getDownloadInfo().getDownloadPath());
        Set<Path> sharedArtifacts = getSharedTrackedArtifacts(driverPackage, packageDirectory);
        List<String> failures = new ArrayList<>();

        for (Library library : driverPackage.getLibraries()) {
            File libraryFile = new File(packageDirectory, library.getLibraryId() + ".jar");
            if (!libraryFile.exists() || sharedArtifacts.contains(normalizedPath(libraryFile.getPath()))) continue;
            if (!libraryFile.isFile()) {
                failures.add(libraryFile.getAbsolutePath());
                continue;
            }
            if (!FileUtil.delete(libraryFile)) failures.add(libraryFile.getAbsolutePath());
        }

        if (!failures.isEmpty()) {
            return new PackageDeletionResult(false, DeletionBlock.DELETE_FAILED, failures);
        }

        PackageChecksumData checksumData = getChecksumData(packageId);
        if (!checksumData.clearChecksums()) failures.add(checksumData.getFile().getAbsolutePath());
        if (!failures.isEmpty()) return new PackageDeletionResult(false, DeletionBlock.DELETE_FAILED, failures);

        downloadState.getDownloadInfo().clearDownloadStatuses();
        return new PackageDeletionResult(true, DeletionBlock.NONE, List.of());
    }

    private Set<Path> getSharedTrackedArtifacts(DriverPackage selectedPackage, File selectedDirectory) {
        Set<Path> sharedArtifacts = new HashSet<>();
        Path selectedPath = normalizedPath(selectedDirectory.getPath());
        List<DriverPackageDownloadInfo> packageInfos = downloadStates.values().stream()
                .map(DriverPackageDownloadState::getDownloadInfo)
                .toList();
        for (Library library : selectedPackage.getLibraries()) {
            String libraryId = library.getLibraryId();
            if (isTrackedArtifactShared(selectedPackage.getId(), libraryId, selectedPath, packageInfos)) {
                sharedArtifacts.add(normalizedPath(new File(selectedDirectory, libraryId + ".jar").getPath()));
            }
        }
        return sharedArtifacts;
    }

    static boolean isTrackedArtifactShared(
            String selectedPackageId,
            String libraryId,
            Path selectedDirectory,
            Collection<? extends DriverPackageDownloadInfo> downloadInfos) {
        for (DriverPackageDownloadInfo otherDownloadInfo : downloadInfos) {
            if (Objects.equals(otherDownloadInfo.getPackageId(), selectedPackageId)) continue;
            if (!Objects.equals(normalizedPath(otherDownloadInfo.getDownloadPath()), selectedDirectory)) continue;

            DriverPackageDownloadInfo.LibraryStatus libraryStatus = otherDownloadInfo.getLibraryStatus(libraryId);
            if (libraryStatus != null && libraryStatus.getDownloadStatus() == DONE) return true;
        }
        return false;
    }

    static Path normalizedPath(String path) {
        if (path == null) return Path.of("");
        try {
            return new File(path).getCanonicalFile().toPath().normalize();
        } catch (IOException e) {
            return new File(path).toPath().toAbsolutePath().normalize();
        }
    }

    static boolean isPackagePathReferenced(String configuredPath, String packagePath) {
        Path configured = normalizedPath(configuredPath);
        Path packageDirectory = normalizedPath(packagePath);
        return configured.equals(packageDirectory) || configured.startsWith(packageDirectory);
    }

    public enum DeletionBlock { NONE, DOWNLOADING, IN_USE, NOT_DOWNLOADED, DELETE_FAILED }

    public record PackageDeletionResult(boolean deleted, DeletionBlock block, List<String> failures) {}

    /** @deprecated Use {@link #openLibraryDashboard(Project, DriverLibraryDashboardOptions)}. */
    @Deprecated
    public void openDownloadDialog(Project project, DatabaseType databaseType, Consumer<String> successCallback) {
        openDownloadDialog(project, databaseType, null, successCallback);
    }

    /** @deprecated Use {@link #openLibraryDashboard(Project, DriverLibraryDashboardOptions)}. */
    @Deprecated
    public void openDownloadDialog(
            Project project,
            DatabaseType databaseType,
            @Nullable CloudConfigProviderFamily providerFamily,
            Consumer<String> successCallback) {
        try {
            List<DriverPackage> driverPackages = getDriverPackages(databaseType, providerFamily);
            Dialogs.show(() -> new DriverDownloadDialog(project, databaseType, driverPackages), (dialog, exitCode) -> {
                when(exitCode == DialogWrapper.OK_EXIT_CODE, () -> successCallback.accept(dialog.getSelectedDownloadPath()));
            });
        } catch (Exception e) {
            showErrorDialog(project, txt("msg.driver.error.DriverLibrariesMetadataDownloadFailed"), e);
        }
    }

    @Override
    public Element getComponentState() {
        Element element = new Element("state");
        Element downloadsElement = newElement(element, "package-downloads");
        Element metadataElement = newElement(element, "package-metadata");
        for (DriverPackageDownloadState downloadState : downloadStates.values()) {
            if (!downloadState.hasPersistedDownloadInfo()) continue;
            Element packageElement = newElement(downloadsElement, "package");
            DriverPackageDownloadInfo downloadInfo = downloadState.getDownloadInfo();
            downloadInfo.writeState(packageElement);
        }

        driverPackageMetadata.writeState(metadataElement);
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        Element downloadsElement = element.getChild("package-downloads");
        for (Element packageElement : childrenOf(downloadsElement, "package")) {
            DriverPackageDownloadState downloadState = new DriverPackageDownloadState();
            DriverPackageDownloadInfo downloadInfo = downloadState.getDownloadInfo();
            downloadInfo.readState(packageElement);
            downloadStates.put(downloadInfo.getPackageId(), downloadState);
        }
        Element metadataElement = element.getChild("package-metadata");
        driverPackageMetadata.readState(metadataElement);
    }
}
