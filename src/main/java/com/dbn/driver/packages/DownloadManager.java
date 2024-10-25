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

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.ApplicationComponentBase;
import com.dbn.common.component.PersistentState;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.component.Components.applicationService;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.driver.packages.DownloadManager.COMPONENT_NAME;

/**
 * Download Manager for tracking the state of driver package downloads.
 * <p>
 * Features:
 * <ol>
 * <li>Tracks download status and timestamp of each JAR within a driver package by package ID and JAR identifier</li>
 * <li>Stores if individual JARs in a package were successfully downloaded and verified</li>
 * <li>Supports cleanup of download records if verification fails for any JAR in a package</li>
 * </ol>
 */
@Slf4j
@State(
    name = COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DownloadManager extends ApplicationComponentBase implements PersistentState {
  public static final String COMPONENT_NAME = "DBNavigator.Application.DownloadManager";

  private final Map<String, Map<String, JarStatus>> packageDownloadStatuses = new ConcurrentHashMap<>();

  public static DownloadManager getInstance() {
    return applicationService(DownloadManager.class);
  }

  public DownloadManager() {
    super(COMPONENT_NAME);
  }

  /**
   * Registers the download status of an individual JAR in a driver package, along with a timestamp.
   *
   * @param packageId Unique ID of the driver package
   * @param jarId Unique ID of the JAR (e.g., "artifactId-version")
   * @param verified True if the download and verification for this JAR succeeded, false otherwise
   */
  public void registerJarDownload(String packageId, String jarId, boolean verified) {
    long downloadTimestamp = System.currentTimeMillis();
    packageDownloadStatuses
        .computeIfAbsent(packageId, k -> new ConcurrentHashMap<>())
        .put(jarId, new JarStatus(verified, downloadTimestamp));
    log.info("Download status for package {} JAR {}: {} at {}", packageId, jarId, verified ? "verified" : "failed", downloadTimestamp);
  }

  /**
   * Checks if all JARs in a driver package have been fully downloaded and verified.
   *
   * @param packageId Unique ID of the driver package
   * @return True if all JARs in the package are verified, false otherwise
   */
  public boolean isPackageDownloaded(String packageId) {
    Map<String, JarStatus> jarStatuses = packageDownloadStatuses.get(packageId);
    if (jarStatuses == null) {
      return false;
    }
    return jarStatuses.values().stream().allMatch(JarStatus::isVerified);
  }

  public void cleanupPackage(String packageId) {
    if (packageDownloadStatuses.remove(packageId) != null) {
      log.info("Download records for package {} removed.", packageId);
    } else {
      log.warn("No records found for package {}.", packageId);
    }
  }

  @Override
  public Element getComponentState() {
    Element element = new Element("state");
    Element downloadsElement = newElement(element, "package-download-statuses");

    for (Map.Entry<String, Map<String, JarStatus>> packageEntry : packageDownloadStatuses.entrySet()) {
      String packageId = packageEntry.getKey();
      Element packageElement = newElement(downloadsElement, "package");
      packageElement.setAttribute("id", packageId);

      for (Map.Entry<String, JarStatus> jarEntry : packageEntry.getValue().entrySet()) {
        String jarId = jarEntry.getKey();
        JarStatus status = jarEntry.getValue();

        Element jarElement = newElement(packageElement, "jar");
        jarElement.setAttribute("id", jarId);
        jarElement.setAttribute("verified", String.valueOf(status.isVerified()));
        jarElement.setAttribute("downloadTimestamp", String.valueOf(status.getDownloadTimestamp()));
      }
    }
    return element;
  }

  @Override
  public void loadComponentState(@NotNull Element element) {
    Element downloadsElement = element.getChild("package-download-statuses");
    if (downloadsElement != null) {
      for (Element packageElement : downloadsElement.getChildren("package")) {
        String packageId = packageElement.getAttributeValue("id");
        Map<String, JarStatus> jarStatuses = new ConcurrentHashMap<>();

        for (Element jarElement : packageElement.getChildren("jar")) {
          String jarId = jarElement.getAttributeValue("id");
          boolean verified = Boolean.parseBoolean(jarElement.getAttributeValue("verified"));
          long downloadTimestamp = Long.parseLong(jarElement.getAttributeValue("downloadTimestamp"));
          jarStatuses.put(jarId, new JarStatus(verified, downloadTimestamp));
        }
        packageDownloadStatuses.put(packageId, jarStatuses);
      }
    }
  }

  /**
   * Inner class to hold the status and timestamp of each JAR download.
   */
  @Getter
  private static class JarStatus {
    private final boolean verified;
    private final long downloadTimestamp;

    public JarStatus(boolean verified, long downloadTimestamp) {
      this.verified = verified;
      this.downloadTimestamp = downloadTimestamp;
    }

  }
}