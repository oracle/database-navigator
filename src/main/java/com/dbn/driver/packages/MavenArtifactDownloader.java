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
import com.dbn.common.util.Files;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.platform.templates.github.DownloadUtil;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.UUID;

public class MavenArtifactDownloader {

    private static final String MAVEN_REPO_URL = "https://repo.maven.apache.org/maven2";

    public static boolean downloadArtifact(String packageId, Library library, String pathLabel) {
        String groupId = library.getGroupId();
        String artifactId = library.getArtifactId();
        String version = library.getVersion();
        String artifactPath = groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar";
        String artifactUrl = MAVEN_REPO_URL + "/" + artifactPath;
        String checksumUrl = artifactUrl + ".sha1";

        try {
            DriverDownloadManager.getInstance().updateJarDownloadStatus(packageId, artifactId + "-" + version, DownloadStatus.PENDING);
            return downloadAndVerify(packageId, artifactUrl, checksumUrl, artifactId, version, pathLabel);
        } catch (IOException e) {
            System.err.println("Download failed for " + artifactId + "-" + version + ": " + e.getMessage());
            return false;
        }
    }

    private static boolean downloadAndVerify(String packageId, String artifactUrl, String checksumUrl, String artifactId, String version, String pathLabel) throws IOException {
        File pluginDir = createPluginDirectory(packageId, pathLabel);
        if (pluginDir == null) return false;

        File outputFile = new File(pluginDir, artifactId + "-" + version + ".jar");

        try {
            DownloadUtil.downloadAtomically(null, artifactUrl, outputFile);
            System.out.println("Artifact downloaded to: " + outputFile.getAbsolutePath());

            String expectedChecksum = getLibraryChecksum(checksumUrl);
            return verifyChecksum(expectedChecksum, outputFile, packageId, artifactId, version);
        } catch (IOException e) {
            deleteFile(outputFile);
            throw e;
        }
    }

    private static File createPluginDirectory(String packageId, String pathLabel) {
        File pluginDir = new File(Files.getPluginDeploymentRoot(), pathLabel + "/" + packageId);
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
                return scanner.nextLine().trim();
            }
        } finally {
            deleteFile(tempFile);
        }
    }

    private static boolean verifyChecksum(String expectedChecksum, File outputFile, String packageId, String artifactId, String version) throws IOException {
        String actualChecksum = Checksum.fromFileContent(outputFile, ChecksumType.SHA_1);
        if (expectedChecksum.equalsIgnoreCase(actualChecksum)) {
            DriverDownloadManager.getInstance().updateJarDownloadStatus(packageId, artifactId + "-" + version, DownloadStatus.DONE);
            return true;
        } else {
            System.err.println("Checksum verification failed! Expected: " + expectedChecksum + ", Actual: " + actualChecksum);
            DriverDownloadManager.getInstance().updateJarDownloadStatus(packageId, artifactId + "-" + version, DownloadStatus.FAILED);
            deleteFile(outputFile);
            return false;
        }
    }

    private static void deleteFile(File file) {
        if (file.exists() && !file.delete()) {
            System.err.println("Failed to delete file: " + file.getAbsolutePath());
        }
    }
}