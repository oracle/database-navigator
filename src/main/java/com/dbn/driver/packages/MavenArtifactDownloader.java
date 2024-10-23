package com.dbn.driver.packages;

import com.intellij.openapi.application.PathManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MavenArtifactDownloader {

  public static void downloadArtifact(String groupId, String artifactId, String version, String pathLabel) throws Exception {
    String repoUrl = "https://repo.maven.apache.org/maven2";

    String artifactPath = groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar";
    String artifactUrl = repoUrl + "/" + artifactPath;

    URL url = new URL(artifactUrl);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");

    int responseCode = connection.getResponseCode();
    if (responseCode == HttpURLConnection.HTTP_OK) {
      File pluginDir = new File(PathManager.getPluginsPath(), pathLabel);
      if (!pluginDir.exists()) {
        boolean dirCreated = pluginDir.mkdirs();
        if (!dirCreated) {
          System.out.println("Failed to create output directory: " + pluginDir.getAbsolutePath());
          return;
        }
      }

      File outputFile = new File(pluginDir, artifactId + "-" + version + ".jar");

      try (InputStream inputStream = connection.getInputStream();
           FileOutputStream outputStream = new FileOutputStream(outputFile)) {

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
          outputStream.write(buffer, 0, bytesRead);
        }

        System.out.println("Artifact downloaded to: " + outputFile.getAbsolutePath());
      }
    } else {
      System.out.println("Failed to download artifact. HTTP response code: " + responseCode);
    }

    connection.disconnect();
  }
}