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

import com.dbn.common.thread.Background;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Files;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class DriverPackageDownloader {

    public static void downloadDriverPackage(Project project, DriverPackage driverPackage) {
        Progress.modal(project, null, true,
                "Downloading Driver Package: " + driverPackage.getId(),
                "",
                indicator -> runProgress(project, indicator, driverPackage));
    }

    private static void runProgress(Project project, ProgressIndicator indicator, DriverPackage driverPackage) {
        String packageId = driverPackage.getId();
        List<Library> libraryList = driverPackage.getLibraries();
        CountDownLatch latch = new CountDownLatch(libraryList.size());
        AtomicBoolean downloadFailed = new AtomicBoolean(false);

        setupIndicator(indicator);

        for (Library library : libraryList) {
            downloadLibraryAsync(project, indicator, packageId, library, downloadFailed, latch, driverPackage.size());
        }

        awaitLatchCompletion(latch, packageId);

        handleCompletion(packageId, libraryList, downloadFailed.get());
    }

    private static void setupIndicator(ProgressIndicator indicator) {
        indicator.setIndeterminate(false);
        indicator.setFraction(0.01);
    }

    private static void downloadLibraryAsync(Project project, ProgressIndicator indicator, String packageId,
                                             Library library, AtomicBoolean downloadFailed, CountDownLatch latch, int fileCount) {
        Background.run(project, () -> {
            if (downloadFailed.get()) {
                latch.countDown();
                return;
            }

            String currentFile = library.getArtifactId() + "-" + library.getVersion() + ".jar";
            if (isAlreadyDownloaded(packageId, library)) {
                System.out.println("Jar " + currentFile + " already downloaded.");
            } else {
                attemptDownload(packageId, library, currentFile, downloadFailed);
            }

            updateProgress(indicator, currentFile, latch, fileCount);
        });
    }

    private static boolean isAlreadyDownloaded(String packageId, Library library) {
        DriverPackageStatus.LibraryStatus status = DriverDownloadManager.getInstance()
                .getJarDownloadStatus(packageId, library.getArtifactId() + "-" + library.getVersion());
        return status.getDownloadStatus().equals(DownloadStatus.DONE);
    }

    private static void attemptDownload(String packageId, Library library, String currentFile, AtomicBoolean downloadFailed) {
        boolean success = MavenArtifactDownloader.downloadArtifact(packageId, library, "drivers");
        if (!success) {
            downloadFailed.set(true);
            System.err.println("Error downloading " + currentFile);
        }
    }

    private static void updateProgress(ProgressIndicator indicator, String currentFile, CountDownLatch latch, int fileCount) {
        latch.countDown();
        indicator.setText("Downloading " + currentFile);
        indicator.setFraction((double) (fileCount-latch.getCount()) / fileCount);
    }

    private static void awaitLatchCompletion(CountDownLatch latch, String packageId) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            System.err.println("Download process interrupted for package: " + packageId);
            Thread.currentThread().interrupt();
        }
    }
    private static void handleCompletion(String packageId, List<Library> libraries, boolean downloadFailed) {
        if (downloadFailed) {
            System.out.println("One or more downloads failed. Cleaning up...");
            cleanupDownloadedJars(packageId, libraries);
            DriverDownloadManager.getInstance().cleanupPackage(packageId);
        } else if (DriverDownloadManager.getInstance().isPackageDownloaded(packageId)) {
            System.out.println("All JARs for package " + packageId + " were successfully downloaded and verified.");
        }
    }

    private static void cleanupDownloadedJars(String packageId, List<Library> libraries) {
        libraries.forEach(library -> {
            File jarFile = getFileForJar(packageId, library.getArtifactId() + "-" + library.getVersion());

            if (jarFile.exists() && !jarFile.delete()) {
                System.err.println("Failed to delete file: " + jarFile.getAbsolutePath());
            } else {
                System.out.println("Deleted file: " + jarFile.getAbsolutePath());
            }
        });
    }

    private static File getFileForJar(String packageId, String jarId) {
        return new File(Files.getPluginDeploymentRoot() + "/drivers/" + packageId, jarId + ".jar");
    }
}