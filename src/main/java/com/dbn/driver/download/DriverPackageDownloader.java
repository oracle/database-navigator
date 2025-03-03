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

import com.dbn.common.message.AsyncMessageCollector;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Files;
import com.dbn.driver.download.metadata.DriverPackage;
import com.dbn.driver.download.metadata.Library;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

public class DriverPackageDownloader {
    private static Consumer<String> updateUI;

    public static void downloadDriverPackage(Project project, DriverPackage driverPackage, AsyncMessageCollector messages, Consumer<String> updateUI) {
        DriverPackageDownloader.updateUI = updateUI;
        Progress.modal(project, null, true,
                "Downloading Driver Package: " + driverPackage.getName(),
                "",
                indicator -> runProgress(indicator, messages, driverPackage, driverPackage.getPath()));
    }

    private static void runProgress(ProgressIndicator indicator, AsyncMessageCollector messages, DriverPackage driverPackage, String path) {
        String packageId = driverPackage.getId();
        List<Library> libraryList = driverPackage.getLibraries();
        CountDownLatch latch = new CountDownLatch(libraryList.size());
        StringBuilder errorMessage = new StringBuilder();

        setupIndicator(indicator);

        errorMessage.setLength(0);
        for (Library library : libraryList) {
            downloadLibraryAsync(indicator, messages, packageId, path, library, errorMessage, latch, driverPackage.size());
        }

        awaitLatchCompletion(latch, messages, packageId);

        handleCompletion(packageId, messages, libraryList, errorMessage.toString());
    }

    private static void setupIndicator(ProgressIndicator indicator) {
        indicator.setIndeterminate(false);
        indicator.setFraction(0.01);
    }

    private static void downloadLibraryAsync(ProgressIndicator indicator, AsyncMessageCollector messages, String packageId, String path,
                                             Library library, StringBuilder errorMessage, CountDownLatch latch, int fileCount) {
        Background.run(() -> {
            if (!errorMessage.toString().isBlank()) {
                latch.countDown();
                return;
            }
            String currentFile = library.getArtifactId() + "-" + library.getVersion() + ".jar";
            if (isAlreadyDownloaded(packageId, library)) {
                System.out.println("Jar " + currentFile + " already downloaded.");
            } else {
                attemptDownload(packageId, messages, library, path, errorMessage);
            }

            updateProgress(indicator, currentFile, latch, fileCount);
        });
    }

    private static boolean isAlreadyDownloaded(String packageId, Library library) {
        DriverPackageStatus.LibraryStatus status = DriverDownloadManager.getInstance()
                .getJarDownloadStatus(packageId, library.getArtifactId() + "-" + library.getVersion());
        return status.getDownloadStatus().equals(DownloadStatus.DONE);
    }

    private static void attemptDownload(String packageId, AsyncMessageCollector messages, Library library, String path, StringBuilder errorMessage) {
        String s = MavenArtifactDownloader.downloadArtifact(packageId, messages, library, path);
        String formattedHtml = s.isBlank()?"":toHtmlFormat(s, 50);
        if (errorMessage.length() == 0) {
            messages.addErrorMessage(formattedHtml);
            errorMessage.append(formattedHtml);
        }
    }

    private static String toHtmlFormat(String text, int maxLineLength) {
        StringBuilder html = new StringBuilder("<html>");
        int length = text.length();

        for (int i = 0; i < length; i += maxLineLength) {
            int end = Math.min(i + maxLineLength, length);
            html.append(text, i, end).append("<br>");
        }

        html.append("</html>");
        return html.toString();
    }

    private static void updateProgress(ProgressIndicator indicator, String currentFile, CountDownLatch latch, int fileCount) {
        latch.countDown();
        indicator.setText("Downloading " + currentFile);
        indicator.setFraction((double) (fileCount-latch.getCount()) / fileCount);
    }

    private static void awaitLatchCompletion(CountDownLatch latch, AsyncMessageCollector messages, String packageId) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            messages.addErrorMessage("Download process interrupted for package: " + packageId);
            Thread.currentThread().interrupt();
        }
    }
    private static void handleCompletion(String packageId, AsyncMessageCollector messages, List<Library> libraries, String errorMessage) {
        if (!errorMessage.isBlank()) {
            messages.addErrorMessage("One or more downloads failed. Cleaning up...");
            cleanupDownloadedJars(packageId, messages, libraries);
            DriverDownloadManager.getInstance().cleanupPackage(packageId);
            ApplicationManager.getApplication().invokeLater(()->{
                updateUI.accept(errorMessage);
            });
        } else if (DriverDownloadManager.getInstance().isPackageDownloaded(packageId, false)) {
            System.out.println("All JARs for package " + packageId + " were successfully downloaded and verified.");
            ApplicationManager.getApplication().invokeLater(()->{
                updateUI.accept("");
            });
        }
    }

    private static void cleanupDownloadedJars(String packageId, AsyncMessageCollector messages, List<Library> libraries) {
        libraries.forEach(library -> {
            File jarFile = getFileForJar(packageId, library.getArtifactId() + "-" + library.getVersion());

            if (jarFile.exists() && !jarFile.delete()) {
                messages.addErrorMessage("Failed to delete file: " + jarFile.getAbsolutePath());
            } else {
                System.out.println("Deleted file: " + jarFile.getAbsolutePath());
            }
        });
    }

    private static File getFileForJar(String packageId, String jarId) {
        return new File(Files.getPluginDeploymentRoot() + "/drivers/" + packageId, jarId + ".jar");
    }
}