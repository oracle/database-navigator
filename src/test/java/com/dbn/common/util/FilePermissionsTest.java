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

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public class FilePermissionsTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void restrictToOwnerRestrictsRegularFile() throws Exception {
        Path file = temporaryFolder.newFile("credentials.txt").toPath();

        FilePermissions.restrictToOwner(file);

        assertOwnerOnly(file, false);
    }

    @Test
    public void restrictToOwnerRestrictsDirectory() throws Exception {
        Path directory = temporaryFolder.newFolder("wallet").toPath();

        FilePermissions.restrictToOwner(directory);

        assertOwnerOnly(directory, true);
    }

    @Test
    public void restrictToOwnerIgnoresMissingPath() throws Exception {
        Path missing = temporaryFolder.getRoot().toPath().resolve("missing");

        FilePermissions.restrictToOwner(missing);
    }

    @Test
    public void restrictToOwnerRejectsSymbolicLink() throws Exception {
        Path target = temporaryFolder.newFile("target").toPath();
        Path link = temporaryFolder.getRoot().toPath().resolve("link");
        try {
            Files.createSymbolicLink(link, target.getFileName());
        } catch (IOException | SecurityException | UnsupportedOperationException e) {
            Assume.assumeTrue("Symbolic links are unavailable: " + e, false);
            return;
        }

        Assert.assertThrows(IOException.class, () -> FilePermissions.restrictToOwner(link));
    }

    @Test
    public void createOwnerOnlyTempEntriesUsesOwnerPermissions() throws Exception {
        Path parent = temporaryFolder.getRoot().toPath();

        Path directory = FilePermissions.createOwnerOnlyTempDirectory(parent, "permissions-");
        Path file = FilePermissions.createOwnerOnlyTempFile(directory, "credentials-", ".tmp");

        assertOwnerOnly(directory, true);
        assertOwnerOnly(file, false);
    }

    private static void assertOwnerOnly(Path path, boolean directory) throws IOException {
        PosixFileAttributeView posixView = Files.getFileAttributeView(
                path, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        if (posixView != null) {
            Set<PosixFilePermission> expected = directory ?
                    Set.of(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE,
                            PosixFilePermission.OWNER_EXECUTE) :
                    Set.of(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE);
            Assert.assertEquals(expected, posixView.readAttributes().permissions());
            return;
        }

        AclFileAttributeView aclView = Files.getFileAttributeView(
                path, AclFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        if (aclView != null) {
            Assert.assertEquals(1, aclView.getAcl().size());
            Assert.assertEquals(aclView.getOwner(), aclView.getAcl().get(0).principal());
            Assert.assertEquals(AclEntryType.ALLOW, aclView.getAcl().get(0).type());
        }
    }
}
