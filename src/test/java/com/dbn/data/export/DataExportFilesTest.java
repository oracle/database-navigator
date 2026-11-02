/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.data.export;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;

public class DataExportFilesTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void getFileKeepsSimpleNameInsideExportDirectory() throws Exception {
        File exportDirectory = temporaryFolder.newFolder("export");

        File file = DataExportFiles.getFile(exportDirectory.getPath(), "safe.csv");

        assertEquals(new File(exportDirectory, "safe.csv").getCanonicalFile(), file);
        assertTrue(file.toPath().startsWith(exportDirectory.getCanonicalFile().toPath()));
    }

    @Test
    public void getFileRejectsParentTraversal() throws Exception {
        File exportDirectory = temporaryFolder.newFolder("export");
        assertThrows(DataExportException.class, () -> DataExportFiles.getFile(exportDirectory.getPath(), "../pwn.csv"));
    }

    @Test
    public void getFileRejectsAbsolutePath() throws Exception {
        File exportDirectory = temporaryFolder.newFolder("export");
        assertThrows(DataExportException.class, () -> DataExportFiles.getFile(exportDirectory.getPath(), temporaryFolder.newFile("pwn.csv").getAbsolutePath()));
    }

    @Test
    public void getFileRejectsBackslashSeparatedPath() throws Exception {
        File exportDirectory = temporaryFolder.newFolder("export");
        assertThrows(DataExportException.class, () -> DataExportFiles.getFile(exportDirectory.getPath(), "..\\pwn.csv"));
    }

    @Test
    public void getFileRejectsMixedTraversalPath() throws Exception {
        File exportDirectory = temporaryFolder.newFolder("export");
        assertThrows(DataExportException.class, () -> DataExportFiles.getFile(exportDirectory.getPath(), "safe/..\\pwn.csv"));
    }

    @Test
    public void getFileRejectsWindowsAbsolutePath() throws Exception {
        File exportDirectory = temporaryFolder.newFolder("export");
        assertThrows(DataExportException.class, () -> DataExportFiles.getFile(exportDirectory.getPath(), "C:\\tmp\\pwn.csv"));
    }

    @Test
    public void writeFileCreatesAndReplacesExportWithOwnerOnlyPermissions() throws Exception {
        Path directory = temporaryFolder.newFolder("export").toPath().toRealPath();
        DataExportInstructions instructions = instructions(directory);
        Path destination = directory.resolve("safe.csv");
        byte[] content = "exported data".getBytes(StandardCharsets.UTF_8);

        DataExportFiles.writeFile(instructions, output -> output.write(content));
        assertArrayEquals(content, Files.readAllBytes(destination));
        DataExportFiles.writeFile(instructions, output -> output.write("replacement".getBytes(StandardCharsets.UTF_8)));
        assertEquals("replacement", Files.readString(destination));
        if (Files.getFileAttributeView(destination, PosixFileAttributeView.class) != null) {
            assertEquals(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
                    Files.getPosixFilePermissions(destination));
        }
    }

    @Test
    public void writeFailurePreservesExistingExport() throws Exception {
        Path directory = temporaryFolder.newFolder("export").toPath().toRealPath();
        Path destination = directory.resolve("safe.csv");
        Files.writeString(destination, "original");

        assertThrows(DataExportException.class, () -> DataExportFiles.writeFile(instructions(directory), output -> {
            output.write("partial".getBytes(StandardCharsets.UTF_8));
            throw new IOException("Generation failed");
        }));
        assertEquals("original", Files.readString(destination));
    }

    @Test
    public void writeFileRejectsExistingAndDanglingDestinationLinks() throws Exception {
        Path directory = temporaryFolder.newFolder("export").toPath().toRealPath();
        Path sentinel = temporaryFolder.newFile("sentinel").toPath().toRealPath();
        Files.writeString(sentinel, "original");
        Path destination = directory.resolve("safe.csv");
        link(destination, sentinel);
        AtomicBoolean invoked = new AtomicBoolean();
        assertThrows(DataExportException.class, () -> DataExportFiles.writeFile(instructions(directory), output -> invoked.set(true)));
        assertFalse(invoked.get());
        assertEquals("original", Files.readString(sentinel));

        Files.delete(destination);
        Path missing = sentinel.resolveSibling("missing");
        link(destination, missing);
        assertThrows(DataExportException.class, () -> DataExportFiles.writeFile(instructions(directory), output -> invoked.set(true)));
        assertFalse(invoked.get());
        assertFalse(Files.exists(missing));
    }

    @Test
    public void writeFileRejectsLinkedParentBeforeGeneratingOutput() throws Exception {
        Path root = temporaryFolder.getRoot().toPath().toRealPath();
        Path outside = Files.createDirectory(root.resolve("outside"));
        Path parent = root.resolve("export");
        link(parent, outside);
        AtomicBoolean invoked = new AtomicBoolean();

        assertThrows(DataExportException.class, () -> DataExportFiles.writeFile(instructions(parent), output -> invoked.set(true)));
        assertFalse(invoked.get());
        assertFalse(Files.exists(outside.resolve("safe.csv")));
    }

    @Test
    public void writeFileRejectsDestinationLinkInsertedDuringGeneration() throws Exception {
        Path directory = temporaryFolder.newFolder("export").toPath().toRealPath();
        Path sentinel = temporaryFolder.newFile("sentinel").toPath().toRealPath();
        Files.writeString(sentinel, "original");
        // Establish symlink support before entering assertThrows.
        Path probe = directory.resolve("probe");
        link(probe, sentinel);
        Files.delete(probe);

        assertThrows(DataExportException.class, () -> DataExportFiles.writeFile(instructions(directory), output -> {
            output.write("confidential".getBytes(StandardCharsets.UTF_8));
            Files.createSymbolicLink(directory.resolve("safe.csv"), sentinel);
        }));
        assertEquals("original", Files.readString(sentinel));
    }

    @Test
    public void writeFileRejectsParentReplacementDuringGeneration() throws Exception {
        Path root = temporaryFolder.getRoot().toPath().toRealPath();
        Path directory = Files.createDirectory(root.resolve("export"));
        Path moved = root.resolve("original-export");
        Path outside = Files.createDirectory(root.resolve("outside"));
        Path sentinel = outside.resolve("safe.csv");
        Files.writeString(sentinel, "original");
        Path probe = root.resolve("probe");
        link(probe, outside);
        Files.delete(probe);

        try {
            assertThrows(DataExportException.class, () -> DataExportFiles.writeFile(instructions(directory), output -> {
                output.write("confidential".getBytes(StandardCharsets.UTF_8));
                Files.move(directory, moved);
                Files.createSymbolicLink(directory, outside);
            }));
            assertEquals("original", Files.readString(sentinel));
            assertFalse(Files.exists(moved.resolve("safe.csv")));
        } finally {
            if (Files.isSymbolicLink(directory)) Files.delete(directory);
        }
    }

    private static DataExportInstructions instructions(Path directory) {
        DataExportInstructions instructions = new DataExportInstructions();
        instructions.setFileLocation(directory.toString());
        instructions.setFileName("safe.csv");
        return instructions;
    }

    private static void link(Path path, Path target) throws IOException {
        try {
            Files.createSymbolicLink(path, target);
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            assumeNoException("Symbolic links are unavailable on this filesystem", e);
        }
    }

    @Test
    public void sanitizeFileNameStripsPathSegmentsFromDefaultName() {
        assertEquals("pwn.csv", DataExportFiles.sanitizeFileName("../pwn.csv"));
        assertEquals("pwn.csv", DataExportFiles.sanitizeFileName("/tmp/pwn.csv"));
        assertEquals("pwn.csv", DataExportFiles.sanitizeFileName("C:\\tmp\\pwn.csv"));
        assertEquals("safe.csv", DataExportFiles.sanitizeFileName("safe.csv"));
    }
}
