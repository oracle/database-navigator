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

import com.dbn.common.checksum.Checksum;
import com.dbn.common.checksum.ChecksumType;
import com.dbn.common.download.Downloads;
import com.dbn.common.util.Files;
import com.dbn.driver.download.metadata.Library;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.io.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.UUID;

@Slf4j
public class MavenArtifactDownloader {

    private static final String MAVEN_REPO_URL = "https://repo.maven.apache.org/maven2";

    public static void downloadArtifact(DownloadSession session, String packageId, Library library) {
        String groupId = library.getGroupId();
        String artifactId = library.getArtifactId();
        String version = library.getVersion();

        String libraryId = library.getLibraryId();
        String artifactPath = library.getArtefactPath();
        String artifactUrl = MAVEN_REPO_URL + "/" + artifactPath;
        String checksumUrl = artifactUrl + ".sha1";

        try {
            DriverDownloadManager downloadManager = DriverDownloadManager.getInstance();
            downloadManager.setDownloadStatus(packageId, libraryId, DownloadStatus.PENDING);
            downloadAndVerify(session, packageId, artifactUrl, checksumUrl, artifactId, version, groupId);

        } catch (Exception e) {
            log.warn("Failed to download artifact '{}'", libraryId, e);
            session.addErrorMessage("Download failed for " + libraryId + ": " + e.getMessage());
        }
    }

    private static void downloadAndVerify(DownloadSession session, String packageId, String artifactUrl, String checksumUrl, String artifactId, String version, String groupId) throws Exception {
        File downloadDir = Files.ensureDirectory(session.getDownloadPath());
        File outputFile = new File(downloadDir, artifactId + "-" + version + ".jar");

        try {
            session.addDownloadedArtifacts(artifactId + "-" + version + ".jar");

            Downloads.downloadAtomically(session, artifactUrl, outputFile);
            log.info("Artifact '{}' downloaded to '{}'", artifactId + "-" + version, outputFile.getAbsolutePath());

            String expectedChecksum = getLibraryChecksum(session, checksumUrl);
            verifyChecksum(expectedChecksum, outputFile, packageId, artifactId, version, groupId);
        } catch (IOException e) {
            deleteFile(outputFile);
            throw e;
        }
    }

    private static String getLibraryChecksum(ProgressIndicator indicator, String checksumUrl) throws IOException {
        File tempFile = FileUtil.createTempFile(UUID.randomUUID().toString(), ".tmp", true);
        try {
            Downloads.downloadAtomically(indicator, checksumUrl, tempFile);
            try (Scanner scanner = new Scanner(tempFile)) {
                // Read the first line and extract the checksum
                String line = scanner.nextLine().trim();
                // Extract only the hexadecimal checksum (e.g., MD5, SHA256, etc.)
                return line.split("\\s+")[0];
            }
        } finally {
            deleteFile(tempFile);
        }
    }

    private static void verifyChecksum(String expectedChecksum, File outputFile, String packageId, String artifactId, String version, String groupId) throws IOException {
        String actualChecksum = Checksum.fromFileContent(outputFile, ChecksumType.SHA_1);

        DriverDownloadManager downloadManager = DriverDownloadManager.getInstance();
        if (expectedChecksum.equals(actualChecksum)) {
            // Update download status
            downloadManager.setDownloadStatus(packageId, artifactId + "-" + version, DownloadStatus.DONE);

            // Append checksum to file
            PackageChecksumData checksumData = downloadManager.getChecksumData(packageId);
            checksumData.addChecksum(artifactId + "-" + version, actualChecksum);
        } else {
            deleteFile(outputFile);
            downloadManager.setDownloadStatus(packageId, artifactId + "-" + version, DownloadStatus.FAILED);
            throw new IOException("Checksum verification failed for " + artifactId + "-" + version);
        }
    }

    private static void deleteFile(File file) {
        if (!FileUtil.delete(file)) {
            log.warn("Failed to delete file '{}'", file.getAbsolutePath());
        }
    }
}