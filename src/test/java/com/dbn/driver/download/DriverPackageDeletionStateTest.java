/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.driver.download;

import org.jdom.Element;
import org.junit.Test;

import java.util.List;

import static com.dbn.driver.download.DownloadStatus.DONE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DriverPackageDeletionStateTest {
    @Test
    public void packagePathGuardMatchesPackageDirectoryAndTrackedJar() {
        String packagePath = "/tmp/driver-libs/oracle";
        assertTrue(DriverDownloadManager.isPackagePathReferenced(packagePath, packagePath));
        assertTrue(DriverDownloadManager.isPackagePathReferenced(packagePath + "/ojdbc.jar", packagePath));
        assertFalse(DriverDownloadManager.isPackagePathReferenced("/tmp/driver-libs/oracle-old", packagePath));
    }

    @Test
    public void deleteGuardRetainsJarsTrackedByAnotherPackageInSameDirectory() {
        DriverPackageDownloadInfo selected = new DriverPackageDownloadInfo("old-package");
        selected.setDownloadPath("/tmp/shared-drivers");
        DriverPackageDownloadInfo other = new DriverPackageDownloadInfo("current-package");
        other.setDownloadPath("/tmp/shared-drivers/.");
        other.ensureLibraryStatus("shared-1.0").setDownloadStatus(DONE);

        assertTrue(DriverDownloadManager.isTrackedArtifactShared(
                selected.getPackageId(),
                "shared-1.0",
                DriverDownloadManager.normalizedPath(selected.getDownloadPath()),
                List.of(selected, other)));
        assertFalse(DriverDownloadManager.isTrackedArtifactShared(
                selected.getPackageId(),
                "other-2.0",
                DriverDownloadManager.normalizedPath(selected.getDownloadPath()),
                List.of(selected, other)));
    }

    @Test
    public void packageDownloadInfoReloadKeepsRecordedPathAndExistingStateShape() {
        DriverPackageDownloadInfo original = new DriverPackageDownloadInfo("oracle-driver");
        original.setDownloadPath("/custom/drivers/oracle");
        DriverPackageDownloadInfo.LibraryStatus library = original.ensureLibraryStatus("ojdbc-23.jar");
        library.setDownloadStatus(DownloadStatus.DONE);
        library.setDownloadTimestamp(123456L);

        Element serialized = new Element("package");
        original.writeState(serialized);

        assertEquals("oracle-driver", serialized.getAttributeValue("id"));
        assertEquals("/custom/drivers/oracle", serialized.getAttributeValue("download-path"));
        assertEquals("library", serialized.getChildren().get(0).getName());

        DriverPackageDownloadInfo reloaded = new DriverPackageDownloadInfo();
        reloaded.readState(serialized);
        assertEquals("/custom/drivers/oracle", reloaded.getDownloadPath());
        assertEquals(DownloadStatus.DONE, reloaded.getLibraryStatus("ojdbc-23.jar").getDownloadStatus());
        assertEquals(123456L, reloaded.getLibraryStatus("ojdbc-23.jar").getDownloadTimestamp());

        reloaded.clearDownloadStatuses();
        assertEquals("/custom/drivers/oracle", reloaded.getDownloadPath());
        assertEquals(DownloadStatus.NEW, reloaded.getLibraryStatus("ojdbc-23.jar").getDownloadStatus());
        assertEquals(0L, reloaded.getLibraryStatus("ojdbc-23.jar").getDownloadTimestamp());
    }
}
