package com.dbn.driver.packages;

import com.dbn.common.util.Files;
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
import java.util.concurrent.CountDownLatch;

public class MavenArtifactDownloader {

  public static void downloadArtifact(Project project, String packageId, String groupId, String artifactId, String version, String pathLabel, CountDownLatch latch) {
    String repoUrl = "https://repo.maven.apache.org/maven2";
    String artifactPath = groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar";
    String artifactUrl = repoUrl + "/" + artifactPath;
    String checksumUrl = artifactUrl + ".sha1"; // URL for the .sha1 file

    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Downloading Maven Artifact " + artifactId + "-" + version + ".jar", true) {
      @Override
      public void run(ProgressIndicator indicator) {
        boolean success = false;
        try {
          success = downloadAndVerify(indicator, packageId, artifactUrl, checksumUrl, artifactId, version, pathLabel);
          if (success) {
            System.out.println("Download and checksum verification succeeded for " + artifactId + "-" + version);
          } else {
            System.out.println("Checksum verification failed for " + artifactId + "-" + version + ". Deleting downloaded artifact...");
          }
        } catch (IOException e) {
          System.out.println("Download failed for " + artifactId + "-" + version + ": " + e.getMessage());
        } finally {
          DownloadManager.getInstance().registerJarDownload(packageId, artifactId + "-" + version, success);
          latch.countDown();
        }
      }
    });
  }

  // Perform the download and verify the SHA-1 checksum
  private static boolean downloadAndVerify(ProgressIndicator indicator, String packageId, String artifactUrl, String checksumUrl, String artifactId, String version, String pathLabel) throws IOException {
    File pluginDir = new File(Files.getPluginDeploymentRoot(), pathLabel+"/"+packageId);
    if (!pluginDir.exists() && !pluginDir.mkdirs()) {
      System.out.println("Failed to create output directory: " + pluginDir.getAbsolutePath());
      return false;
    }

    File outputFile = new File(pluginDir, artifactId + "-" + version + ".jar");

    try {
      DownloadUtil.downloadAtomically(indicator, artifactUrl, outputFile);
      System.out.println("Artifact downloaded to: " + outputFile.getAbsolutePath());

      String expectedChecksum = downloadChecksum(checksumUrl);
      if (expectedChecksum != null) {
        String actualChecksum = calculateSHA1Checksum(outputFile);
        if (expectedChecksum.equalsIgnoreCase(actualChecksum)) {
          return true;
        } else {
          System.out.println("Checksum verification failed! Expected: " + expectedChecksum + ", Actual: " + actualChecksum);
          deleteFile(outputFile);
          return false;
        }
      } else {
        System.out.println("Failed to download the checksum file.");
        deleteFile(outputFile);
        return false;
      }
    } catch (IOException e) {
      deleteFile(outputFile);
      throw e;
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

  private static void deleteFile(File file) {
    if (file.exists()) {
      if (file.delete()) {
        System.out.println("Deleted file: " + file.getAbsolutePath());
      } else {
        System.out.println("Failed to delete file: " + file.getAbsolutePath());
      }
    }
  }
}