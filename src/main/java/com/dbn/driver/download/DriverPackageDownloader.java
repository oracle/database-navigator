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
import com.intellij.openapi.util.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.function.Consumer;

import static com.dbn.driver.download.MavenArtifactDownloader.downloadArtifact;
import static com.dbn.nls.NlsResources.txt;

@Slf4j
public class DriverPackageDownloader {
    private Consumer<String> updateUI;

    public void downloadDriverPackage(Project project, DriverPackage driverPackage, Consumer<String> updateUI) {
        this.updateUI = updateUI;
        DriverDownloadManager downloadManager = getDownloadManager();

        Progress.modal(project, null, true,
                txt("prc.connection.title.DownloadingDrivers"),
                txt("prc.connection.text.DownloadingDriverPackages", driverPackage.getName()),
                indicator -> {
                    int downloadCount = driverPackage.getLibraries().size();
                    String packageId = driverPackage.getId();
                    String downloadPath = downloadManager.getDownloadPath(packageId);

                    DownloadSession downloadSession = new DownloadSession(indicator)
                            .withDownloadSize(downloadCount)
                            .withDownloadPath(downloadPath)
                            .withLatchControl();
                    downloadDriverPackage(downloadSession, driverPackage);
                });
    }

    private void downloadDriverPackage(DownloadSession session, DriverPackage driverPackage) {
        String packageId = driverPackage.getId();
        PackageChecksumData checksumData = getDownloadManager().getChecksumData(packageId);
        checksumData.readChecksums();

        for (Library library : driverPackage.getLibraries()) {
            if (ProgressMonitor.isProgressCancelled()) break;
            downloadDriverLibrary(session, packageId, library);
        }

        awaitLatchCompletion(session, packageId);
        checksumData.writeChecksums();

        handleCompletion(packageId, session);
    }

    private void downloadDriverLibrary(
            DownloadSession session,
            String packageId,
            Library library) {
        Background.run(() -> {
            if (session.hasErrors()) {
                session.countDown();
                return;
            }
            String currentFile = library.getArtifactId() + "-" + library.getVersion() + ".jar";
            if (isAlreadyDownloaded(packageId, library)) {
                log.info("Library '{}' download skipped. Already downloaded.", currentFile);
            } else {
                downloadArtifact(session, packageId, library);
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
        session.updateProgress("Downloaded " + currentFile);
    }

    private void awaitLatchCompletion(DownloadSession session, String packageId) {
        try {
            while (!session.isComplete()) {
                if (ProgressMonitor.isProgressCancelled()) {
                    session.addInfoMessage(txt("msg.connection.info.DownloadCanceledForPackage", packageId));
                    break;
                }
                if (session.awaitCompletion()) {
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Error waiting on download completion for package: {}", packageId, e);
            session.addErrorMessage(txt("msg.connection.error.DownloadInterruptedForPackage", packageId));
        }
    }

    private void handleCompletion(String packageId, DownloadSession session) {
        DriverDownloadManager downloadManager = getDownloadManager();
        if (session.hasErrors()) {
            log.warn("Package '{}' download and verification failed.", packageId);
            session.addErrorMessage(txt("msg.connection.error.DownloadsFailedCleaningUp"));
            cleanupDownloadedJars(session);
            downloadManager.cleanupPackage(packageId);
            ApplicationManager.getApplication().invokeLater(()->{
                updateUI.accept(toHtmlFormat(session.getErrorMessages().get(0).getText(), 50));
            });
        } else if (downloadManager.isPackageDownloaded(packageId)) {
            log.info("Package '{}' download and verification successfully completed.", packageId);
            ApplicationManager.getApplication().invokeLater(()->{
                updateUI.accept("");
            });
        } else {
            log.info("Package '{}' download cancelled.", packageId);
            ApplicationManager.getApplication().invokeLater(()->{
                updateUI.accept(null);
            });
            cleanupDownloadedJars(session);
            downloadManager.cleanupPackage(packageId);
        }
    }

    private void cleanupDownloadedJars(DownloadSession session) {
        session.getDownloadedArtifacts().forEach(library -> {
            File libraryFile = new File(session.getDownloadPath() + File.separator + library);
            String filePath = libraryFile.getAbsolutePath();

            if (FileUtil.delete(libraryFile)) {
                log.info("Deleted library file '{}'", filePath);
            } else {
                log.warn("Failed to delete library file '{}'", filePath);
                session.addErrorMessage(txt("msg.connection.error.FailedToDeleteFile", libraryFile.getAbsolutePath()));
            }
        });
    }

    private static @NotNull DriverDownloadManager getDownloadManager() {
        return DriverDownloadManager.getInstance();
    }
}
