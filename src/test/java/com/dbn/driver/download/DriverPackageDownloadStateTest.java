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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DriverPackageDownloadStateTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void packageDownloadStateOwnsChecksumAndDownloadActivity() throws Exception {
        DriverPackageDownloadState downloadState = new DriverPackageDownloadState("oracle-driver");
        PackageChecksumData checksumData = new PackageChecksumData("oracle-driver", temporaryFolder.newFile("checksums.txt"));
        downloadState.setChecksumData(checksumData);

        assertSame(checksumData, downloadState.getChecksumData());
        assertTrue(downloadState.beginPackageDownload());
        assertFalse(downloadState.beginPackageDownload());
        assertTrue(downloadState.isPackageDownloading());
        downloadState.endPackageDownload();
        assertFalse(downloadState.isPackageDownloading());
    }

    @Test
    public void persistedDownloadInfoRetainsItsLegacyXmlShape() {
        Element legacyPackage = new Element("package")
                .setAttribute("id", "oracle-driver")
                .setAttribute("download-path", "/drivers/oracle");
        legacyPackage.addContent(new Element("library")
                .setAttribute("id", "ojdbc-23")
                .setAttribute("download-status", "DONE")
                .setAttribute("download-timestamp", "42"));

        DriverPackageDownloadState downloadState = new DriverPackageDownloadState();
        downloadState.getDownloadInfo().readState(legacyPackage);
        downloadState.beginPackageDownload();

        Element savedPackage = new Element("package");
        downloadState.getDownloadInfo().writeState(savedPackage);
        DriverPackageDownloadState reloadedDownloadState = new DriverPackageDownloadState();
        reloadedDownloadState.getDownloadInfo().readState(savedPackage);

        assertEquals("oracle-driver", reloadedDownloadState.getDownloadInfo().getPackageId());
        assertEquals("/drivers/oracle", reloadedDownloadState.getDownloadInfo().getDownloadPath());
        assertEquals(DownloadStatus.DONE, reloadedDownloadState.getDownloadInfo().getLibraryStatus("ojdbc-23").getDownloadStatus());
        assertEquals(42, reloadedDownloadState.getDownloadInfo().getLibraryStatus("ojdbc-23").getDownloadTimestamp());
        assertFalse(reloadedDownloadState.isPackageDownloading());
    }

    @Test
    public void checksumOnlyEntriesDoNotCreatePersistedDownloadRecords() throws Exception {
        DriverPackageDownloadState downloadState = new DriverPackageDownloadState("oracle-driver");
        File checksumFile = temporaryFolder.newFile("checksums.txt");
        downloadState.setChecksumData(new PackageChecksumData("oracle-driver", checksumFile));

        assertFalse(downloadState.hasPersistedDownloadInfo());

        downloadState.getDownloadInfo().setDownloadPath("/drivers/oracle");
        assertTrue(downloadState.hasPersistedDownloadInfo());
    }
}
