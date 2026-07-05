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

package com.dbn.common.util;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.createTempFile;

@UtilityClass
public class FilePermissions {
    private static final Set<PosixFilePermission> OWN_DIRECTORY_PERMISSIONS = Set.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE);

    private static final Set<PosixFilePermission> OWN_FILE_PERMISSIONS = Set.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE);


    public static void restrictToOwner(File file) {
        if (!file.exists()) return;

        try {
            restrictToOwnerPosix(file);
        } catch (UnsupportedOperationException e) {
            restrictToOwnerLegacy(file);
        }
    }

    @SneakyThrows
    private static void restrictToOwnerPosix(File file) {
        if (!file.exists()) return;

        Path filePath = file.toPath();
        PosixFileAttributeView view = Files.getFileAttributeView(filePath, PosixFileAttributeView.class);
        if (view == null) throw new UnsupportedOperationException("File attribute view not supported");

        FileAttribute<Set<PosixFilePermission>> permissions = file.isDirectory() ?
                ownDirectoryPermissions() :
                ownFilePermissions();

        view.setPermissions(permissions.value());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void restrictToOwnerLegacy(File file) {
        if (!file.exists()) return;

        file.setReadable(false, false);
        file.setReadable(true, true);

        file.setWritable(false, false);
        file.setWritable(true, true);

        if (file.isDirectory()) {
            file.setExecutable(false, false);
            file.setExecutable(true, true);
        } else {
            file.setExecutable(false, false);
            file.setExecutable(false, true);
        }
    }

    public static FileAttribute<Set<PosixFilePermission>> ownFilePermissions() {
        return PosixFilePermissions.asFileAttribute(OWN_FILE_PERMISSIONS);
    }

    public static FileAttribute<Set<PosixFilePermission>> ownDirectoryPermissions() {
        return PosixFilePermissions.asFileAttribute(OWN_DIRECTORY_PERMISSIONS);
    }

    public static Path createOwnerOnlyTempDirectory(String prefix) throws IOException {
        try {
            return createTempDirectory(prefix, ownDirectoryPermissions());
        } catch (UnsupportedOperationException e) {
            Path tempDirectory = createTempDirectory(prefix);
            restrictToOwner(tempDirectory.toFile());
            return tempDirectory;
        }
    }

    public static Path createOwnerOnlyTempDirectory(Path parentDirectory, String prefix) throws IOException {
        try {
            return createTempDirectory(parentDirectory, prefix, ownDirectoryPermissions());
        } catch (UnsupportedOperationException e) {
            Path tempDirectory = createTempDirectory(parentDirectory, prefix);
            restrictToOwner(tempDirectory.toFile());
            return tempDirectory;
        }
    }

    public static Path createOwnerOnlyTempFile(Path directory, String prefix, String suffix) throws IOException {
        try {
            return createTempFile(directory, prefix, suffix, ownFilePermissions());
        } catch (UnsupportedOperationException e) {
            Path tempFile = createTempFile(directory, prefix, suffix);
            restrictToOwner(tempFile.toFile());
            return tempFile;
        }
    }
}
