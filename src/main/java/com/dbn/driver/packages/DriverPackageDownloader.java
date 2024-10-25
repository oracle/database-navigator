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

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class DriverPackageDownloader {

  public static void downloadDriverPackage(Project project, DriverPackage driverPackage) {
    List<Library> libraryList = driverPackage.getLibraries();
    String packageId = driverPackage.getId();
    CountDownLatch latch = new CountDownLatch(libraryList.size());

    for (Library library : libraryList) {
      try {
        MavenArtifactDownloader.downloadArtifact(
            project,
            packageId,
            library.getGroupId(),
            library.getArtifactId(),
            library.getVersion(),
            "drivers",
            latch);
      } catch (Exception e) {
        System.out.println(e.getMessage());
      }
    }

    try {
      latch.await(); // Wait until all downloads complete
    } catch (InterruptedException e) {
      System.out.println("Download process interrupted for package: " + packageId);
      Thread.currentThread().interrupt();
    }

    if (DownloadManager.getInstance().isPackageDownloaded(packageId)) {
      System.out.println("All JARs for package " + packageId + " were successfully downloaded and verified.");
    } else {
      System.out.println("One or more JARs in package " + packageId + " failed verification. Cleaning up...");
      cleanupDownloadedJars(packageId, libraryList);
      DownloadManager.getInstance().cleanupPackage(packageId);
    }
  }

  /**
   * Deletes all downloaded JAR files for the given package.
   *
   * @param packageId the ID of the driver package
   * @param libraries the list of libraries (JARs) to be cleaned up
   */
  private static void cleanupDownloadedJars(String packageId, List<Library> libraries) {
    for (Library library : libraries) {
      String jarId = library.getArtifactId() + "-" + library.getVersion();
      File jarFile = getFileForJar(packageId, jarId);

      if (jarFile.exists()) {
        if (jarFile.delete()) {
          System.out.println("Deleted file: " + jarFile.getAbsolutePath());
        } else {
          System.out.println("Failed to delete file: " + jarFile.getAbsolutePath());
        }
      }
    }
  }

  /**
   * Constructs the file path for a downloaded JAR file based on packageId and jarId.
   *
   * @param packageId Unique ID of the driver package
   * @param jarId Unique ID of the JAR (e.g., "artifactId-version")
   * @return File representing the path to the downloaded JAR
   */
  private static File getFileForJar(String packageId, String jarId) {
    String baseDirectory = Files.getPluginDeploymentRoot() + "/drivers/" + packageId;
    return new File(baseDirectory, jarId + ".jar");
  }
}
