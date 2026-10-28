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

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicBoolean;

/** Runtime state for one tracked driver package. Persisted download details retain the legacy XML format. */
@Getter
@Setter
public class DriverPackageDownloadState {
    private final DriverPackageDownloadInfo downloadInfo;
    private volatile PackageChecksumData checksumData;
    private final AtomicBoolean downloading = new AtomicBoolean();

    public DriverPackageDownloadState() {
        this(new DriverPackageDownloadInfo());
    }

    DriverPackageDownloadState(String packageId) {
        this(new DriverPackageDownloadInfo(packageId));
    }

    private DriverPackageDownloadState(DriverPackageDownloadInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
    }

    public PackageChecksumData getChecksumData() {
        PackageChecksumData currentChecksumData = checksumData;
        if (currentChecksumData == null) {
            synchronized (this) {
                currentChecksumData = checksumData;
                if (currentChecksumData == null) {
                    currentChecksumData = new PackageChecksumData(downloadInfo.getPackageId());
                    checksumData = currentChecksumData;
                }
            }
        }
        return currentChecksumData;
    }

    public boolean beginPackageDownload() {
        return downloading.compareAndSet(false, true);
    }

    public void endPackageDownload() {
        downloading.set(false);
    }

    public boolean isPackageDownloading() {
        return downloading.get();
    }

    /** Checksum-only package entries do not need a package-downloads XML record. */
    public boolean hasPersistedDownloadInfo() {
        return downloadInfo.getDownloadPath() != null || !downloadInfo.getLibraryStatuses().isEmpty();
    }
}
