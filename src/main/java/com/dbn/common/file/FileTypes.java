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

package com.dbn.common.file;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.fileTypes.UnknownFileType;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import static com.dbn.common.util.Commons.coalesce;

@UtilityClass
public class FileTypes {

    public static @NonNull PlainTextFileType getTextFileType() {
        return PlainTextFileType.INSTANCE;
    }

    public static FileType getJavaFileType() {
        return coalesce(
                () -> resolveFileType("java"),
                () -> getTextFileType());
    }

    public static FileType getClassFileType() {
        return coalesce(
                () -> resolveFileType("class"),
                () -> getTextFileType());
    }

    public static FileType getXmlFileType() {
        return coalesce(
                () -> resolveFileType("xml"),
                () -> getTextFileType());
    }

    public static FileType getDtdFileType() {
        return coalesce(
                () -> resolveFileType("dtd"),
                () -> getTextFileType());
    }

    public static FileType getJsonFileType() {
        return coalesce(
                () -> resolveFileType("json"),
                () -> resolveFileType("js"),
                () -> getTextFileType());
    }

    /**
     * Custom internal utility to resolve file types to be used in a "coalesce" fallback
     * (avoid internal fallback to {@link UnknownFileType})
     */
    @Nullable
    private static FileType resolveFileType(String extension) {
        FileType fileType = getFileType(extension);
        if (fileType instanceof UnknownFileType) return null;
        return fileType;
    }

    @NotNull
    public static FileType getFileType(String extension) {
        FileTypeManager fileTypeManager = FileTypeManager.getInstance();
        return fileTypeManager.getFileTypeByExtension(extension);
    }
}
