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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.dbn.common.util.LinkRejectingFiles.copyFileNoFollow;
import static com.dbn.common.util.LinkRejectingFiles.createDirectoriesNoFollow;
import static com.dbn.common.util.LinkRejectingFiles.deleteIfExistsNoFollow;
import static com.dbn.common.util.LinkRejectingFiles.deleteTreeNoFollow;
import static com.dbn.common.util.LinkRejectingFiles.validateTreeNoFollow;
import static com.dbn.common.util.LinkRejectingFiles.writeStringNoFollow;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;

public class LinkRejectingFilesTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void ordinaryOutputCanBeWrittenCopiedAndCleaned() throws Exception {
        Path root = root();
        Path config = root.resolve("mcp-dist/server/config/mcp-config.yaml");
        writeStringNoFollow(config, "configuration", StandardCharsets.UTF_8);
        Path copy = config.resolveSibling("README.md");
        copyFileNoFollow(config, copy);
        assertEquals("configuration", Files.readString(copy));
        writeStringNoFollow(copy, "replacement", StandardCharsets.UTF_8);
        assertEquals("replacement", Files.readString(copy));
        deleteTreeNoFollow(root.resolve("mcp-dist"));
        assertFalse(Files.exists(config));
    }

    @Test
    public void linkedDestinationsCannotChangeSentinel() throws Exception {
        Path root = root();
        Path sentinel = root.resolve("sentinel");
        Files.writeString(sentinel, "original");
        Path output = Files.createDirectories(root.resolve("mcp-dist/server"));
        for (String name : List.of("mcp-config.yaml", "README.md", "server.jar", "Main.java")) {
            Path destination = output.resolve(name);
            link(destination, sentinel);
            assertThrows(IOException.class, () -> writeStringNoFollow(destination, "changed", StandardCharsets.UTF_8));
            assertThrows(IOException.class, () -> copyFileNoFollow(sentinel, destination));
            assertThrows(IOException.class, () -> deleteIfExistsNoFollow(destination));
            assertEquals("original", Files.readString(sentinel));
            assertTrue(Files.isSymbolicLink(destination));
        }
    }

    @Test
    public void linkedAncestorsCannotCreateOrDeleteOutsideOutput() throws Exception {
        Path root = root();
        Path outside = Files.createDirectory(root.resolve("outside"));
        Path sentinel = outside.resolve("sentinel");
        Files.writeString(sentinel, "original");
        for (String name : List.of("mcp-dist", "source-project", "config", "wallet")) {
            Path ancestor = root.resolve(name);
            link(ancestor, outside);
            Path destination = ancestor.resolve("nested/mcp-config.yaml");
            assertThrows(IOException.class, () -> createDirectoriesNoFollow(destination.getParent()));
            assertThrows(IOException.class, () -> writeStringNoFollow(destination, "changed", StandardCharsets.UTF_8));
            assertThrows(IOException.class, () -> copyFileNoFollow(sentinel, destination));
            assertThrows(IOException.class, () -> deleteTreeNoFollow(ancestor));
            assertFalse(Files.exists(outside.resolve("nested")));
            assertEquals("original", Files.readString(sentinel));
        }
    }

    @Test
    public void danglingLinksAreRejected() throws Exception {
        Path root = root();
        Path missing = root.resolve("missing");
        Path destination = root.resolve("mcp-config.yaml");
        link(destination, missing);
        assertThrows(IOException.class, () -> writeStringNoFollow(destination, "changed", StandardCharsets.UTF_8));
        assertThrows(IOException.class, () -> validateTreeNoFollow(destination));
        assertFalse(Files.exists(missing));
    }

    @Test
    public void cleanupRejectsLinkedDescendantsBeforeDeletingFiles() throws Exception {
        Path root = root();
        Path sentinel = root.resolve("sentinel");
        Files.writeString(sentinel, "original");
        Path output = Files.createDirectory(root.resolve("output"));
        Path regularFile = output.resolve("regular");
        Files.writeString(regularFile, "keep");
        link(output.resolve("linked"), sentinel);
        assertThrows(IOException.class, () -> deleteTreeNoFollow(output));
        assertTrue(Files.exists(regularFile));
        assertEquals("original", Files.readString(sentinel));
    }

    private Path root() throws IOException {
        // Canonicalize platform temp aliases, such as /var -> /private/var on macOS.
        return temporaryFolder.getRoot().toPath().toRealPath();
    }

    private static void link(Path link, Path target) throws IOException {
        try {
            Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | IOException | SecurityException e) {
            assumeNoException("Symbolic links are unavailable on this filesystem", e);
        }
    }
}
