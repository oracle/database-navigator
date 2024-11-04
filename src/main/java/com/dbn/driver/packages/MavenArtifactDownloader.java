/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * This software is dual-licensed to you under the Universal Permissive License
 *  (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl or Apache License
 *   2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
 *   either license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.dbn.driver.packages;

import com.dbn.common.util.Files;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.platform.templates.github.DownloadUtil;
import com.intellij.openapi.progress.ProgressIndicator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
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
        String actualChecksum = calculateSHA1Checksum(outputFile);
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

    private static String calculateSHA1Checksum(File file) throws IOException {
        try (InputStream fis = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IOException("Failed to calculate checksum: " + e.getMessage(), e);
        }
    }

    private static void deleteFile(File file) {
        if (file.exists() && !file.delete()) {
            System.err.println("Failed to delete file: " + file.getAbsolutePath());
        }
    }
}