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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.stream.Stream;

import static com.dbn.nls.NlsResources.txt;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * Performs filesystem operations while rejecting links in every existing path component,
 * including intentional links in ancestor directories. Missing components are allowed.
 * Paths are made absolute and normalized before use; no containment boundary is enforced.
 * Path-based checks cannot prevent concurrent replacement of an ancestor directory.
 */
@UtilityClass
public final class LinkRejectingFiles {
    public static void validatePathNoFollow(Path path) throws IOException {
        Path absolute = path.toAbsolutePath().normalize();
        Path current = absolute.getRoot();
        for (Path segment : absolute) {
            current = current.resolve(segment);
            BasicFileAttributes attributes;
            try {
                attributes = Files.readAttributes(current, BasicFileAttributes.class, NOFOLLOW_LINKS);
            } catch (NoSuchFileException e) {
                // Remaining descendants cannot exist until this component is created.
                return;
            }
            // Real-path comparison also rejects redirected directories (e.g. Windows junctions).
            if (attributes.isSymbolicLink() || attributes.isOther() ||
                    !current.toRealPath().equals(current)) {
                throw new IOException(txt("msg.shared.exception.UnsafeFilePath", current));
            }
        }
    }

    public static void validateTreeNoFollow(Path directory) throws IOException {
        directory = directory.toAbsolutePath().normalize();
        validatePathNoFollow(directory);
        if (!Files.exists(directory, NOFOLLOW_LINKS)) return;
        // Files.walk does not follow links, but each entry must still be rejected explicitly.
        try (Stream<Path> stream = Files.walk(directory)) {
            for (Path path : stream.toList()) validatePathNoFollow(path);
        }
    }

    public static void createDirectoriesNoFollow(Path directory) throws IOException {
        Path absolute = directory.toAbsolutePath().normalize();
        Path current = absolute.getRoot();
        for (Path segment : absolute) {
            current = current.resolve(segment);
            validatePathNoFollow(current);
            try {
                Files.createDirectory(current);
            } catch (FileAlreadyExistsException e) {
                // Validate the winner as well if another process created this directory.
            }
            validatePathNoFollow(current);
            if (!Files.isDirectory(current, NOFOLLOW_LINKS)) {
                throw new IOException(txt("msg.shared.exception.UnsafeFilePath", current));
            }
        }
    }

    public static void writeStringNoFollow(Path file, String content, Charset charset) throws IOException {
        file = file.toAbsolutePath().normalize();
        createDirectoriesNoFollow(file.getParent());
        validatePathNoFollow(file);
        // NOFOLLOW_LINKS protects the leaf even when replaced after validation.
        Files.writeString(file, content, charset, CREATE, TRUNCATE_EXISTING, WRITE, NOFOLLOW_LINKS);
    }

    public static void copyFileNoFollow(Path source, Path target) throws IOException {
        source = source.toAbsolutePath().normalize();
        target = target.toAbsolutePath().normalize();
        validatePathNoFollow(source);
        createDirectoriesNoFollow(target.getParent());
        validatePathNoFollow(target);
        // REPLACE_EXISTING replaces the destination entry instead of following a leaf link.
        Files.copy(source, target, REPLACE_EXISTING, COPY_ATTRIBUTES, NOFOLLOW_LINKS);
    }

    public static void deleteIfExistsNoFollow(Path path) throws IOException {
        path = path.toAbsolutePath().normalize();
        validatePathNoFollow(path);
        Files.deleteIfExists(path);
    }

    public static void deleteTreeNoFollow(Path directory) throws IOException {
        directory = directory.toAbsolutePath().normalize();
        validateTreeNoFollow(directory);
        if (!Files.exists(directory, NOFOLLOW_LINKS)) return;
        try (Stream<Path> stream = Files.walk(directory)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) deleteIfExistsNoFollow(path);
        }
    }
}
