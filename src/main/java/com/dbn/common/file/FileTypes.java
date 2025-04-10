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
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.util.Commons.coalesce;

@UtilityClass
public class FileTypes {

    public static FileType getJavaFileType() {
        return coalesce(
                () -> getFileTypeByExtension("java"),
                () -> PlainTextFileType.INSTANCE);
    }

    public static FileType getClassFileType() {
        return coalesce(
                () -> getFileTypeByExtension("class"),
                () -> PlainTextFileType.INSTANCE);
    }

    public static FileType getJsonFileType() {
        return coalesce(
                () -> getFileTypeByExtension("json"),
                () -> getFileTypeByExtension("js"),
                () -> PlainTextFileType.INSTANCE);
    }

    @NotNull
    private static FileType getFileTypeByExtension(String extension) {
        FileTypeManager fileTypeManager = FileTypeManager.getInstance();
        return fileTypeManager.getFileTypeByExtension(extension);
    }
}
