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
import com.dbn.driver.download.metadata.LibraryChecksum;
import com.intellij.openapi.util.io.FileUtil;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.util.Strings.isNotEmptyOrSpaces;
import static com.dbn.driver.download.DriverDownloadManager.getDriverPackageChecksumsLocation;

@Getter
public class PackageChecksumData {
    private final String packageId;
    private final File file;
    private final Map<String, LibraryChecksum> checksums = new ConcurrentHashMap<>();
    private final Set<File> invalidChecksums = new HashSet<>();

    public PackageChecksumData(String packageId) {
        this(packageId, new File(getDriverPackageChecksumsLocation(), packageId + ".txt"));
    }

    PackageChecksumData(String packageId, File file) {
        this.packageId = packageId;
        this.file = file;
    }

    public void addChecksum(String libraryId, ChecksumType type, String checksum) {
        checksums.put(libraryId, new LibraryChecksum(type, checksum));
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
             .map(l -> l.split("\\s+"))
             .forEach(this::readChecksum);
    }

    @SneakyThrows
    public void writeChecksums() {
        StringBuilder builder = new StringBuilder();
        Map<String, LibraryChecksum> checksums = new TreeMap<>(this.checksums); // sort
        checksums.forEach((k, v) -> builder.append(k)
                .append(" ").append(v.getType().name())
                .append(" ").append(v.getValue())
                .append("\n"));
        FileUtil.writeToFile(file, builder.toString());
    }

    public boolean verifyChecksums(File packageDir) {
        invalidChecksums.clear();
        if (checksums.isEmpty()) return false;

        for (String libraryId : checksums.keySet()) {
            LibraryChecksum checksum = checksums.get(libraryId);
            File libraryFile = new File(packageDir, libraryId + ".jar");
            if (!libraryFile.exists() || checksum.getType() == null) {
                invalidChecksums.add(libraryFile);
                continue;
            }

            ChecksumType type = checksum.getType();
            String actualChecksum = Checksum.fromFileContent(libraryFile, type);
            if (!Checksum.verifyChecksum(checksum.getValue(), actualChecksum, type)) {
                invalidChecksums.add(libraryFile);
            }
        }
        return invalidChecksums.isEmpty();
    }

    public boolean verifyStrongChecksums(File packageDir) {
        invalidChecksums.clear();
        if (checksums.isEmpty()) return false;

        File[] libraryFiles = packageDir.listFiles(file -> file.isFile() && file.getName().endsWith(".jar"));
        if (libraryFiles == null || libraryFiles.length == 0) return false;

        for (File libraryFile : libraryFiles) {
            String libraryId = getLibraryId(libraryFile);
            LibraryChecksum checksum = checksums.get(libraryId);
            if (checksum == null || !checksum.isStrong()) {
                invalidChecksums.add(libraryFile);
                continue;
            }

            ChecksumType type = checksum.getType();
            String actualChecksum = Checksum.fromFileContent(libraryFile, type);
            if (!Checksum.verifyChecksum(checksum.getValue(), actualChecksum, type)) {
                invalidChecksums.add(libraryFile);
            }
        }
        return invalidChecksums.isEmpty();
    }

    private static String getLibraryId(File libraryFile) {
        String name = libraryFile.getName();
        return name.endsWith(".jar") ? name.substring(0, name.length() - 4) : name;
    }

    private void readChecksum(String[] values) {
        if (values.length < 2) return;

        String libraryId = values[0];
        if (values.length == 2) {
            // Legacy DBN files stored only SHA-1 digests.
            checksums.put(libraryId, new LibraryChecksum(ChecksumType.SHA_1, values[1]));
            return;
        }

        ChecksumType type = readChecksumType(values[1]);
        checksums.put(libraryId, new LibraryChecksum(type, values[2]));
    }

    private static ChecksumType readChecksumType(String value) {
        try {
            return ChecksumType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
