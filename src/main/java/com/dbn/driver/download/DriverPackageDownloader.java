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

import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Progress;
import com.dbn.driver.download.metadata.DriverPackage;
import com.dbn.driver.download.metadata.Library;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public class DriverPackageDownloader {
    private Consumer<String> updateUI;

    public void downloadDriverPackage(Project project, DriverPackage driverPackage, Consumer<String> updateUI) {
        this.updateUI = updateUI;
        DriverDownloadManager downloadManager = getDownloadManager();

        Progress.modal(project, null, true,
                "Downloading Drivers",
                "Downloading driver packages for " + driverPackage.getName(),
                indicator -> {
                    int downloadCount = driverPackage.getLibraries().size();
                    String packageId = driverPackage.getId();
                    String downloadPath = downloadManager.getDownloadPath(packageId);

                    DownloadSession downloadSession = new DownloadSession(indicator)
                            .withDownloadSize(downloadCount)
                            .withDownloadPath(downloadPath)
                            .withLatchControl();
                    runProgress(downloadSession, driverPackage);
                });
    }

    private void runProgress(DownloadSession session, DriverPackage driverPackage) {
        String packageId = driverPackage.getId();
        List<Library> libraryList = driverPackage.getLibraries();

        for (Library library : libraryList) {
            if(ProgressMonitor.isProgressCancelled()) break;
            downloadLibraryAsync(session, packageId, library);
        }

        awaitLatchCompletion(session, packageId);
        writeChecksumData(packageId);

        handleCompletion(packageId, session);
    }

    private static void writeChecksumData(String packageId) {
        PackageChecksumData checksumData = getDownloadManager().getChecksumData(packageId);
        checksumData.writeChecksums();
    }

    private void downloadLibraryAsync(
            DownloadSession session,
            String packageId,
            Library library) {
        Background.run(() -> {
            if (!session.getErrorMessages().isEmpty()) {
                session.countDown();
                return;
            }
            String currentFile = library.getArtifactId() + "-" + library.getVersion() + ".jar";
            if (isAlreadyDownloaded(packageId, library)) {
                System.out.println("Jar " + currentFile + " already downloaded.");
            } else {
                attemptDownload(session, packageId, library);
            }

            updateProgress(session, currentFile);
        });
    }

    private boolean isAlreadyDownloaded(String packageId, Library library) {
        DriverDownloadManager downloadManager = getDownloadManager();
        String libraryId = library.getLibraryId();

        DownloadStatus downloadStatus = downloadManager.getDownloadStatus(packageId, libraryId);
        return downloadStatus == DownloadStatus.DONE;
    }

    private void attemptDownload(DownloadSession session, String packageId, Library library) {
        String s = MavenArtifactDownloader.downloadArtifact(session, packageId, library);
        if (!s.isEmpty()) {
            session.addErrorMessage(s);
        }
    }

    private String toHtmlFormat(String text, int maxLineLength) {
        StringBuilder html = new StringBuilder("<html>");
        int length = text.length();

        for (int i = 0; i < length; i += maxLineLength) {
            int end = Math.min(i + maxLineLength, length);
            html.append(text, i, end).append("<br>");
        }

        html.append("</html>");
        return html.toString();
    }

    private void updateProgress(DownloadSession session, String currentFile) {
        session.countDown();
        session.updateProgress("Downloading " + currentFile);
    }

    private void awaitLatchCompletion(DownloadSession session, String packageId) {
        try {
            while (!session.isComplete()) {
                if (ProgressMonitor.isProgressCancelled()) {
                    session.addInfoMessage("Download process cancelled for package: " + packageId);
                    break;
                }
                if (session.awaitCompletion()) {
                    break;
                }
            }
        } catch (Exception e) {
            session.addErrorMessage("Download process interrupted for package: " + packageId);
        }
    }

    private void handleCompletion(String packageId, DownloadSession session) {
        DriverDownloadManager downloadManager = getDownloadManager();
        if (!session.getErrorMessages().isEmpty()) {
            session.addErrorMessage("One or more downloads failed. Cleaning up...");
            cleanupDownloadedJars(session);
            downloadManager.cleanupPackage(packageId);
            ApplicationManager.getApplication().invokeLater(()->{
                updateUI.accept(toHtmlFormat(session.getErrorMessages().get(0).getText(), 50));
            });
        } else if (downloadManager.isPackageDownloaded(packageId)) {
            System.out.println("All JARs for package " + packageId + " were successfully downloaded and verified.");
            ApplicationManager.getApplication().invokeLater(()->{
                updateUI.accept("");
            });
        } else {
            System.out.println("Download process cancelled for package: " + packageId);
            cleanupDownloadedJars(session);
            downloadManager.cleanupPackage(packageId);
        }
    }

    private void cleanupDownloadedJars(DownloadSession session) {
        session.getDownloadedArtifacts().forEach(library -> {
            File jarFile = new File(session.getDownloadPath()+"/"+library);

            if (jarFile.exists() && !jarFile.delete()) {
                session.addErrorMessage("Failed to delete file: " + jarFile.getAbsolutePath());
            } else {
                System.out.println("Deleted file: " + jarFile.getAbsolutePath());
            }
        });
    }

    private static @NotNull DriverDownloadManager getDownloadManager() {
        return DriverDownloadManager.getInstance();
    }
}