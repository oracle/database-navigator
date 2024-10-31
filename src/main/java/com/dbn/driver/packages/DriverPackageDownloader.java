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
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import java.io.File;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class DriverPackageDownloader {

    private static final int MAX_CONCURRENT_DOWNLOADS = 5;

    public static void downloadDriverPackage(Project project, DriverPackage driverPackage) {
        List<Library> libraryList = driverPackage.getLibraries();
        String packageId = driverPackage.getId();
        CountDownLatch latch = new CountDownLatch(libraryList.size());
        AtomicBoolean downloadFailed = new AtomicBoolean(false);

        DownloadManager.getInstance().cleanupPackage(packageId);
        ExecutorService executorService = Executors.newFixedThreadPool(Math.min(libraryList.size(), MAX_CONCURRENT_DOWNLOADS));

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Downloading Driver Package: " + packageId, true) {
            @Override
            public void run(ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setFraction(0.01);
                double totalFiles = libraryList.size();

                for (Library library : libraryList) {
                    executorService.submit(() -> {
                        if (downloadFailed.get()) {
                            latch.countDown();
                            return;
                        }

                        String currentFile = library.getArtifactId() + "-" + library.getVersion() + ".jar";
                        indicator.setText("Downloading " + currentFile);

                        boolean success = MavenArtifactDownloader.downloadArtifact(
                                project,
                                packageId,
                                library.getGroupId(),
                                library.getArtifactId(),
                                library.getVersion(),
                                "drivers",
                                indicator
                        );

                        if (!success) {
                            downloadFailed.set(true);
                            System.err.println("Error downloading " + currentFile);
                        }

                        latch.countDown();
                        indicator.setFraction((totalFiles - latch.getCount()) / totalFiles);
                    });
                }

                try {
                    latch.await();
                    executorService.shutdown();
                } catch (InterruptedException e) {
                    System.err.println("Download process interrupted for package: " + packageId);
                    Thread.currentThread().interrupt();
                }

                handleCompletion(packageId, libraryList, downloadFailed.get());
            }
        });
    }

    private static void handleCompletion(String packageId, List<Library> libraries, boolean downloadFailed) {
        if (downloadFailed) {
            System.out.println("One or more downloads failed. Cleaning up...");
            cleanupDownloadedJars(packageId, libraries);
            DownloadManager.getInstance().cleanupPackage(packageId);
        } else if (DownloadManager.getInstance().isPackageDownloaded(packageId, libraries.size())) {
            System.out.println("All JARs for package " + packageId + " were successfully downloaded and verified.");
        }
    }

    private static void cleanupDownloadedJars(String packageId, List<Library> libraries) {
        libraries.forEach(library -> {
            File jarFile = getFileForJar(packageId, library.getArtifactId() + "-" + library.getVersion());

            if (jarFile.exists() && !jarFile.delete()) {
                System.err.println("Failed to delete file: " + jarFile.getAbsolutePath());
            } else {
                System.out.println("Deleted file: " + jarFile.getAbsolutePath());
            }
        });
    }

    private static File getFileForJar(String packageId, String jarId) {
        return new File(Files.getPluginDeploymentRoot() + "/drivers/" + packageId, jarId + ".jar");
    }
}