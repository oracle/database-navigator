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

import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.dbn.common.util.Files.normalizePath;
import static com.dbn.common.util.Strings.isEmptyOrSpaces;
import static com.dbn.nls.NlsResources.txt;

@UtilityClass
public class DataExportFiles {
    public static File getFile(String fileLocation, String fileName) throws DataExportException {
        String normalizedFileName = normalizeFileName(fileName);
        Path basePath = toPath(fileLocation).toAbsolutePath().normalize();
        Path filePath = basePath.resolve(normalizedFileName).normalize();

        if (!filePath.startsWith(basePath)) {
            throw new DataExportException(txt("msg.dataExport.error.InvalidFilePath"));
        }
        return containedFile(basePath, filePath);
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
