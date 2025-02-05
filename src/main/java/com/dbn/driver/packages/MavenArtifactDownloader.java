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

import com.dbn.common.checksum.Checksum;
import com.dbn.common.checksum.ChecksumType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.platform.templates.github.DownloadUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.UUID;

public class MavenArtifactDownloader {

    private static final String MAVEN_REPO_URL = "https://repo.maven.apache.org/maven2";
    private static final String DRIVER_PACKAGES_PATH = "/Users/ayoub/IdeaProjects/dbn-internal/build/idea-sandbox/plugins/dbn-plugin/driver-packages";


    public static String downloadArtifact(String packageId, Library library, String pathLabel) {
        String groupId = library.getGroupId();
        String artifactId = library.getArtifactId();
        String version = library.getVersion();
        String artifactPath = groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar";
        String artifactUrl = MAVEN_REPO_URL + "/" + artifactPath;
        String checksumUrl = artifactUrl + ".sha1";
        System.out.println(library);
        System.out.println(checksumUrl);
        try {
            DriverDownloadManager.getInstance().updateJarDownloadStatus(packageId, artifactId + "-" + version, DownloadStatus.PENDING);
            return downloadAndVerify(packageId, artifactUrl, checksumUrl, artifactId, version, groupId, pathLabel);
        } catch (IOException e) {
            System.err.println("Download failed for " + artifactId + "-" + version + ": " + e.getMessage());
            return e.getMessage();
        }
    }

    private static String downloadAndVerify(String packageId, String artifactUrl, String checksumUrl, String artifactId, String version, String groupId, String pathLabel) throws IOException {
        File pluginDir = createPluginDirectory(pathLabel);
        if (pluginDir == null) return "Couldn't create or access download directory";

        File outputFile = new File(pluginDir, artifactId + "-" + version + ".jar");

        try {
            DownloadUtil.downloadAtomically(null, artifactUrl, outputFile);
            System.out.println("Artifact downloaded to: " + outputFile.getAbsolutePath());

            String expectedChecksum = getLibraryChecksum(checksumUrl);
            return verifyChecksum(expectedChecksum, outputFile, packageId, artifactId, version, groupId);
        } catch (IOException e) {
            deleteFile(outputFile);
            throw e;
        }
    }

    private static File createPluginDirectory(String pathLabel) {
        File pluginDir = new File(pathLabel);
        synchronized (MavenArtifactDownloader.class) {
            if (!pluginDir.exists() && !pluginDir.mkdirs()) {
                System.err.println("Failed to create output directory: " + pluginDir.getAbsolutePath());
                return null;
            }
            System.out.println("Created directory: " + pluginDir.getAbsolutePath());
        }
        return pluginDir;
    }

    private static String getLibraryChecksum(String checksumUrl) throws IOException {
        File tempFile = FileUtil.createTempFile(UUID.randomUUID().toString(), ".tmp", true);
        try {
            DownloadUtil.downloadAtomically(null, checksumUrl, tempFile);
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

    private static String verifyChecksum(String expectedChecksum, File outputFile, String packageId, String artifactId, String version, String groupId) throws IOException {
        String actualChecksum = Checksum.fromFileContent(outputFile, ChecksumType.SHA_1);

        if (expectedChecksum.equalsIgnoreCase(actualChecksum)) {
            // Update download status
            DriverDownloadManager.getInstance().updateJarDownloadStatus(packageId, artifactId + "-" + version, DownloadStatus.DONE);

            // Append checksum to file
            appendChecksumToFile(packageId, groupId, artifactId, version, actualChecksum);
            return "";
        } else {
            System.err.println("Checksum verification failed! Expected: " + expectedChecksum + ", Actual: " + actualChecksum);
            DriverDownloadManager.getInstance().updateJarDownloadStatus(packageId, artifactId + "-" + version, DownloadStatus.FAILED);
            deleteFile(outputFile);
            return "Checksum verification failed for " + artifactId + "-" + version;
        }
    }

    private static void appendChecksumToFile(String packageId, String groupId, String artifactId, String version, String checksum) {
        File checksumFile = new File(DRIVER_PACKAGES_PATH, packageId + ".txt");
        String entry = artifactId + "-" + version + " " + checksum;

        synchronized (MavenArtifactDownloader.class) { // Ensure thread safety
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(checksumFile, true))) {
                writer.write(entry);
                writer.newLine();
            } catch (IOException e) {
                System.err.println("Failed to write checksum to file: " + e.getMessage());
            }
        }
    }

    private static void deleteFile(File file) {
        if (file.exists() && !file.delete()) {
            System.err.println("Failed to delete file: " + file.getAbsolutePath());
        }
    }
}