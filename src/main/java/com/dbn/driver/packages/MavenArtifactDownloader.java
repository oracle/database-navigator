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

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.platform.templates.github.DownloadUtil;
import com.intellij.openapi.progress.ProgressIndicator;

import java.io.File;
import java.io.IOException;

public class MavenArtifactDownloader {

  public static void downloadArtifact(Project project, String groupId, String artifactId, String version, String pathLabel) {
    String repoUrl = "https://repo.maven.apache.org/maven2";
    String artifactPath = groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar";
    String artifactUrl = repoUrl + "/" + artifactPath;

    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Downloading Maven Artifact "+artifactId + "-" + version + ".jar", true) {
      @Override
      public void run(ProgressIndicator indicator) {
        try {
          File pluginDir = new File(PathManager.getPluginsPath(), pathLabel);
          if (!pluginDir.exists() && !pluginDir.mkdirs()) {
            System.out.println("Failed to create output directory: " + pluginDir.getAbsolutePath());
            return;
          }

          File outputFile = new File(pluginDir, artifactId + "-" + version + ".jar");

          DownloadUtil.downloadAtomically(indicator, artifactUrl, outputFile);
          System.out.println("Artifact downloaded to: " + outputFile.getAbsolutePath());

        } catch (IOException e) {
          e.getMessage();
          System.out.println("Failed to download artifact.");
        }
      }
    });
  }
}