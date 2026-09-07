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

import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.EnumSet;
import java.util.List;
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
        try {
            restrictToOwner(file.toPath());
        } catch (IOException e) {
            throw new IllegalStateException("Could not restrict file permissions: " + file, e);
        }
    }

    /**
     * Restricts a local path to its owner. POSIX permissions are used where available,
     * Windows ACLs are reduced to one owner allow entry, and the legacy File API is the
     * final fallback for providers that expose neither attribute view.
     *
     * @throws IOException when the path cannot be secured
     */
    public static void restrictToOwner(Path path) throws IOException {
        if (path == null) return;
        if (Files.isSymbolicLink(path)) {
            throw new IOException("Refusing to change permissions through a symbolic link: " + path);
        }
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.notExists(path, LinkOption.NOFOLLOW_LINKS)) return;
            throw new IOException("Could not access path while changing permissions: " + path);
        }

        if (restrictToOwnerPosix(path) || restrictToOwnerAcl(path)) return;
        if (!restrictToOwnerLegacyInternal(path.toFile())) {
            throw new IOException("File permission provider could not restrict path to its owner: " + path);
        }
    }

    private static boolean restrictToOwnerPosix(Path path) throws IOException {
        PosixFileAttributeView view = Files.getFileAttributeView(
                path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        if (view == null) return false;

        try {
            view.setPermissions(Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) ?
                    OWN_DIRECTORY_PERMISSIONS : OWN_FILE_PERMISSIONS);
            return true;
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }

    private static boolean restrictToOwnerAcl(Path path) throws IOException {
        AclFileAttributeView view = Files.getFileAttributeView(
                path, AclFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        if (view == null) return false;

        UserPrincipal owner = view.getOwner();
        Set<AclEntryPermission> permissions = EnumSet.of(
                AclEntryPermission.READ_DATA,
                AclEntryPermission.WRITE_DATA,
                AclEntryPermission.APPEND_DATA,
                AclEntryPermission.READ_ATTRIBUTES,
                AclEntryPermission.WRITE_ATTRIBUTES,
                AclEntryPermission.READ_NAMED_ATTRS,
                AclEntryPermission.WRITE_NAMED_ATTRS,
                AclEntryPermission.DELETE,
                AclEntryPermission.READ_ACL,
                AclEntryPermission.SYNCHRONIZE);
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            permissions = EnumSet.copyOf(permissions);
            permissions.add(AclEntryPermission.EXECUTE);
            permissions.add(AclEntryPermission.DELETE_CHILD);
        }

        AclEntry entry = AclEntry.newBuilder()
                .setType(AclEntryType.ALLOW)
                .setPrincipal(owner)
                .setPermissions(permissions)
                .build();
        try {
            view.setAcl(List.of(entry));
            return true;
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }

    private static boolean restrictToOwnerLegacyInternal(File file) {
        if (!file.exists()) return true;

        boolean success = true;
        success &= file.setReadable(false, false);
        success &= file.setReadable(true, true);
        success &= file.setWritable(false, false);
        success &= file.setWritable(true, true);

        if (file.isDirectory()) {
            success &= file.setExecutable(false, false);
            success &= file.setExecutable(true, true);
        } else {
            success &= file.setExecutable(false, false);
            success &= file.setExecutable(false, true);
        }
        return success;
    }

    public static void restrictToOwnerLegacy(File file) {
        if (!restrictToOwnerLegacyInternal(file)) {
            throw new IllegalStateException("Could not restrict file permissions: " + file);
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
            restrictToOwner(tempDirectory);
            return tempDirectory;
        }
    }

    public static Path createOwnerOnlyTempDirectory(Path parentDirectory, String prefix) throws IOException {
        try {
            return createTempDirectory(parentDirectory, prefix, ownDirectoryPermissions());
        } catch (UnsupportedOperationException e) {
            Path tempDirectory = createTempDirectory(parentDirectory, prefix);
            restrictToOwner(tempDirectory);
            return tempDirectory;
        }
    }

    public static Path createOwnerOnlyTempFile(Path directory, String prefix, String suffix) throws IOException {
        try {
            return createTempFile(directory, prefix, suffix, ownFilePermissions());
        } catch (UnsupportedOperationException e) {
            Path tempFile = createTempFile(directory, prefix, suffix);
            restrictToOwner(tempFile);
            return tempFile;
        }
    }
}
