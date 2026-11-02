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

import com.dbn.common.routine.ThrowableConsumer;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.dbn.common.util.FilePermissions.createOwnerOnlyTempDirectory;
import static com.dbn.common.util.FilePermissions.createOwnerOnlyTempFile;
import static com.dbn.common.util.Files.normalizePath;
import static com.dbn.common.util.LinkRejectingFiles.deleteIfExistsNoFollow;
import static com.dbn.common.util.LinkRejectingFiles.validatePathNoFollow;
import static com.dbn.common.util.Strings.isEmptyOrSpaces;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

@UtilityClass
public class DataExportFiles {
    public static File getFile(String fileLocation, String fileName) throws DataExportException {
        Path filePath = resolveFilePath(fileLocation, fileName);
        return containedFile(filePath.getParent(), filePath);
    }

    /**
     * Portable export hardening: generate in a private staging directory, then replace the
     * destination entry instead of reopening it for writing. Path checks cannot prevent
     * concurrent ancestor replacement; this does not provide directory-handle binding.
     */
    public static void writeFile(DataExportInstructions instructions, ThrowableConsumer<OutputStream, IOException> writer)
            throws DataExportException {
        // Keep the selected pathname: canonicalizing first would hide existing links.
        Path destination = resolveFilePath(instructions.getFileLocation(), instructions.getFileName());
        Path stagingDirectory = null;
        Path stagingFile = null;
        try {
            validatePathNoFollow(destination);
            // Stage outside the selected export folder so its other writers cannot replace
            // the temporary file between creation, permission restriction, and opening.
            stagingDirectory = createOwnerOnlyTempDirectory("dbn-export-");
            stagingDirectory = stagingDirectory.toRealPath();
            stagingFile = createOwnerOnlyTempFile(stagingDirectory, "data-", ".tmp");
            validatePathNoFollow(stagingFile);
            try (OutputStream output = Files.newOutputStream(stagingFile, WRITE, TRUNCATE_EXISTING, NOFOLLOW_LINKS)) {
                writer.accept(output);
            }

            validatePathNoFollow(stagingFile);
            validatePathNoFollow(destination);
            try {
                Files.move(stagingFile, destination, ATOMIC_MOVE, REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException | FileAlreadyExistsException e) {
                // Some providers cannot atomically move across filesystems or replace an existing target.
                // Preserve ordinary export and overwrite support with a checked non-atomic move.
                conditionallyLog(e);
                validatePathNoFollow(stagingFile);
                validatePathNoFollow(destination);
                Files.move(stagingFile, destination, REPLACE_EXISTING);
            }
        } catch (IOException e) {
            conditionallyLog(e);
            throw new DataExportException(txt("msg.dataExport.error.CreateExportFileFailed", e.getMessage()));
        } finally {
            cleanupStagingPath(stagingFile);
            cleanupStagingPath(stagingDirectory);
        }
    }

    private static void cleanupStagingPath(Path path) {
        if (path == null) return;
        try {
            deleteIfExistsNoFollow(path);
        } catch (IOException e) {
            conditionallyLog(e);
        }
    }

    private static Path resolveFilePath(String fileLocation, String fileName) throws DataExportException {
        String normalizedFileName = normalizeFileName(fileName);
        Path basePath = toPath(fileLocation).toAbsolutePath().normalize();
        Path filePath = basePath.resolve(normalizedFileName).normalize();
        if (!filePath.startsWith(basePath)) {
            throw new DataExportException(txt("msg.dataExport.error.InvalidFilePath"));
        }
        return filePath;
    }

    public static String sanitizeFileName(String fileName) {
        if (isEmptyOrSpaces(fileName)) return fileName;

        String normalizedFileName = normalizePath(fileName.trim());
        return new File(normalizedFileName).getName();
    }

    private static String normalizeFileName(String fileName) throws DataExportException {
        if (isEmptyOrSpaces(fileName)) throw new DataExportException(txt("msg.dataExport.error.InvalidExportFileName"));

        String normalizedFileName = normalizePath(fileName.trim());
        Path filePath = toPath(normalizedFileName);
        if (filePath.isAbsolute() || filePath.getNameCount() != 1 || ".".equals(normalizedFileName) || "..".equals(normalizedFileName)) {
            throw new DataExportException(txt("msg.dataExport.error.InvalidExportFileName"));
        }
        return normalizedFileName;
    }

    private static Path toPath(String path) throws DataExportException {
        try {
            return Paths.get(path);
        } catch (InvalidPathException e) {
            throw new DataExportException(txt("msg.dataExport.error.InvalidExportFilePath"));
        }
    }

    private static File containedFile(Path basePath, Path filePath) throws DataExportException {
        try {
            Path canonicalBasePath = basePath.toFile().getCanonicalFile().toPath();
            File canonicalFile = filePath.toFile().getCanonicalFile();
            if (!canonicalFile.toPath().startsWith(canonicalBasePath)) {
                throw new DataExportException(txt("msg.dataExport.error.InvalidFilePath"));
            }
            return canonicalFile;
        } catch (IOException e) {
            throw new DataExportException(txt("msg.dataExport.error.InvalidExportFilePath"));
        }
    }
}
