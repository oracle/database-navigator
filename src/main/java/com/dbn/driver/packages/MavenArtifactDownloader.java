/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * This software is dual-licensed to you under the Universal Permissive License
 * (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl or Apache License
 * 2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
 * either license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.dbn.driver.packages;

import com.dbn.common.util.Files;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.platform.templates.github.DownloadUtil;
import com.intellij.openapi.progress.ProgressIndicator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Scanner;

public class MavenArtifactDownloader {

  private static final int MAX_RETRIES = 3; // Maximum number of attempts

  public static void downloadArtifact(Project project, String groupId, String artifactId, String version, String pathLabel) {
    String repoUrl = "https://repo.maven.apache.org/maven2";
    String artifactPath = groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar";
    String artifactUrl = repoUrl + "/" + artifactPath;
    String checksumUrl = artifactUrl + ".sha1"; // URL for the .sha1 file

    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Downloading Maven Artifact " + artifactId + "-" + version + ".jar", true) {
      @Override
      public void run(ProgressIndicator indicator) {
        int attempt = 0;
        boolean success = false;

        while (attempt < MAX_RETRIES && !success) {
          attempt++;
          System.out.println("Download attempt " + attempt + " for " + artifactId + "-" + version + ".jar");

          try {
            // Perform the download and checksum verification
            success = downloadAndVerify(indicator, artifactUrl, checksumUrl, artifactId, version, pathLabel);
            if (success) {
              System.out.println("Download and checksum verification succeeded on attempt " + attempt);
            } else {
              System.out.println("Checksum verification failed on attempt " + attempt);
            }
          } catch (IOException e) {
            System.out.println("Download failed on attempt " + attempt + ": " + e.getMessage());
          }

          if (!success && attempt < MAX_RETRIES) {
            System.out.println("Retrying download...");
          }
        }

        if (!success) {
          System.out.println("Download failed after " + MAX_RETRIES + " attempts.");
        }
      }
    });
  }

  // Perform the download and verify the SHA-1 checksum
  private static boolean downloadAndVerify(ProgressIndicator indicator, String artifactUrl, String checksumUrl, String artifactId, String version, String pathLabel) throws IOException {
    File pluginDir = new File(Files.getPluginDeploymentRoot(), pathLabel);
    if (!pluginDir.exists() && !pluginDir.mkdirs()) {
      System.out.println("Failed to create output directory: " + pluginDir.getAbsolutePath());
      return false;
    }

    File outputFile = new File(pluginDir, artifactId + "-" + version + ".jar");

    // Download the artifact file
    DownloadUtil.downloadAtomically(indicator, artifactUrl, outputFile);
    System.out.println("Artifact downloaded to: " + outputFile.getAbsolutePath());

    // Download the SHA-1 checksum file
    String expectedChecksum = downloadChecksum(checksumUrl);
    if (expectedChecksum != null) {
      // Calculate the actual checksum of the downloaded file
      String actualChecksum = calculateSHA1Checksum(outputFile);
      if (expectedChecksum.equalsIgnoreCase(actualChecksum)) {
        return true; // Checksum verification succeeded
      } else {
        System.out.println("Checksum verification failed! Expected: " + expectedChecksum + ", Actual: " + actualChecksum);
        return false; // Checksum verification failed
      }
    } else {
      System.out.println("Failed to download the checksum file.");
      return false;
    }
  }

  // Method to download the SHA-1 checksum file
  private static String downloadChecksum(String checksumUrl) throws IOException {
    URL url = new URL(checksumUrl);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");

    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
      try (Scanner scanner = new Scanner(connection.getInputStream())) {
        return scanner.nextLine().trim(); // Return the first line of the checksum file
      }
    } else {
      throw new IOException("Failed to download checksum file from: " + checksumUrl);
    }
  }

  // Method to calculate the SHA-1 checksum of the downloaded file
  private static String calculateSHA1Checksum(File file) throws IOException {
    try (InputStream fis = new FileInputStream(file)) {
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      byte[] buffer = new byte[4096];
      int bytesRead;
      while ((bytesRead = fis.read(buffer)) != -1) {
        digest.update(buffer, 0, bytesRead);
      }
      byte[] hashBytes = digest.digest();

      // Convert the byte array into a hex string
      StringBuilder sb = new StringBuilder();
      for (byte b : hashBytes) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (Exception e) {
      throw new IOException("Failed to calculate checksum: " + e.getMessage(), e);
    }
  }
}