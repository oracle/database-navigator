/*
 * Copyright 2025 Oracle and/or its affiliates
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

import com.dbn.common.checksum.Checksum;
import com.dbn.common.checksum.ChecksumType;
import com.intellij.openapi.util.io.FileUtil;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.driver.download.DriverDownloadManager.getDriverPackageChecksumsLocation;

@Getter
public class PackageChecksumData {
    private final String packageId;
    private final File file;
    private final Map<String, String> checksums = new ConcurrentHashMap<>();
    private final Set<File> invalidChecksums = new HashSet<>();

    public PackageChecksumData(String packageId) {
        this.packageId = packageId;
        this.file = new File(getDriverPackageChecksumsLocation(), packageId + ".txt");
    }

    public void addChecksum(String libraryId, String checksum) {
        checksums.put(libraryId, checksum);
    }

    public boolean fileExists() {
        return file.exists();
    }

    public boolean hasInvalidChecksums() {
        return !invalidChecksums.isEmpty();
    }

    @SneakyThrows
    public void readChecksums() {
        if (!file.exists()) return;
        checksums.clear();
        List<String> lines = FileUtil.loadLines(file);
        lines.stream()
             .filter(l -> isNotEmptyOrSpaces(l))
             .map(l -> l.split(" "))
             .forEach(l -> checksums.put(l[0], l[1]));
    }

    @SneakyThrows
    public void writeChecksums() {
        StringBuilder builder = new StringBuilder();
        Map<String, String> checksums = new TreeMap<>(this.checksums); // sort
        checksums.forEach((k, v) -> builder.append(k).append(" ").append(v).append("\n"));
        FileUtil.writeToFile(file, builder.toString());
    }

    public boolean verifyChecksums(File packageDir) {
        invalidChecksums.clear();

        for (String libraryId : checksums.keySet()) {
            String checksum = checksums.get(libraryId);
            File libraryFile = new File(packageDir, libraryId + ".jar");
            if (libraryFile.exists()) {
                String actualChecksum = Checksum.fromFileContent(libraryFile, ChecksumType.SHA_1);
                if (!Objects.equals(checksum, actualChecksum)) {
                    invalidChecksums.add(libraryFile);
                }
            } else {
                invalidChecksums.add(libraryFile);
            }
        }
        return invalidChecksums.isEmpty();
    }
}
