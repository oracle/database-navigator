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

import com.dbn.common.checksum.ChecksumType;
import com.dbn.common.download.Downloads;
import com.dbn.common.util.Files;
import com.dbn.driver.download.metadata.Library;
import com.dbn.driver.download.metadata.LibraryChecksum;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.io.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

import static com.dbn.nls.NlsResources.txt;

@Slf4j
public class MavenArtifactDownloader {

    public static void downloadArtifact(DownloadSession session, String packageId, Library library) {
        String libraryId = library.getLibraryId();
        String artifactPath = library.getArtefactPath();
        String artifactUrl = MavenRepositories.CENTRAL_URL + "/" + artifactPath;

        try {
            DriverDownloadManager downloadManager = DriverDownloadManager.getInstance();
            downloadManager.setDownloadStatus(packageId, libraryId, DownloadStatus.PENDING);
            downloadAndVerify(session, packageId, artifactUrl, library);

        } catch (ProcessCanceledException ignored) {
            session.addInfoMessage(txt("msg.connection.info.DownloadCanceledForPackage", packageId));
        } catch (Exception e) {
            log.warn("Failed to download artifact '{}'", libraryId, e);
            session.addErrorMessage(txt("msg.connection.error.DownloadFailedForLibrary", libraryId, e));
        }
    }

    private static void downloadAndVerify(DownloadSession session, String packageId, String artifactUrl, Library library) throws Exception {
        File downloadDir = Files.ensureDirectory(session.getDownloadPath());
        File outputFile = new File(downloadDir, library.getFileName());

        try {
            session.addDownloadedArtifacts(library.getFileName());

            Downloads.downloadAtomically(session, artifactUrl, outputFile);
            log.info("Artifact '{}' downloaded to '{}'", library.getLibraryId(), outputFile.getAbsolutePath());

            LibraryChecksum checksum = MavenArtifactIntegrityVerifier.verify(session, artifactUrl, outputFile, library);
            registerVerifiedChecksum(checksum, outputFile, packageId, library);
        } catch (IOException e) {
            deleteFile(outputFile);
            throw e;
        }
    }

    private static void registerVerifiedChecksum(LibraryChecksum checksum, File outputFile, String packageId, Library library) throws IOException {
        DriverDownloadManager downloadManager = DriverDownloadManager.getInstance();
        String libraryId = library.getLibraryId();
        ChecksumType checksumType = checksum.getType();
        String checksumValue = checksum.getValue();

        if (checksumType != null && checksum.hasValue()) {
            // Update download status
            downloadManager.setDownloadStatus(packageId, libraryId, DownloadStatus.DONE);

            // Append checksum to file
            PackageChecksumData checksumData = downloadManager.getChecksumData(packageId);
            checksumData.addChecksum(libraryId, checksumType, checksumValue);
        } else {
            deleteFile(outputFile);
            downloadManager.setDownloadStatus(packageId, libraryId, DownloadStatus.FAILED);
            throw new IOException("Checksum verification failed for " + libraryId);
        }
    }

    private static void deleteFile(File file) {
        if (!FileUtil.delete(file)) {
            log.warn("Failed to delete file '{}'", file.getAbsolutePath());
        }
    }
}
