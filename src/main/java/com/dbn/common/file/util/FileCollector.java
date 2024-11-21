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

package com.dbn.common.file.util;

import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

class FileCollector extends VirtualFileVisitor {
    private final FileTypeManager fileTypeManager = FileTypeManager.getInstance();
    private final Map<String, VirtualFile> bucket = new HashMap<>();
    private final FileSearchRequest request;

    private FileCollector(FileSearchRequest request) {
        this.request = request;
    }

    public static FileCollector create(FileSearchRequest request) {
        return new FileCollector(request);
    }

    public boolean visitFile(@NotNull VirtualFile file) {
        boolean fileIgnored = fileTypeManager.isFileIgnored(file.getName());
        if (!fileIgnored) {
            if (file.isDirectory()) {
                return true;
            } else {
                if (request.matches(file)) {
                    bucket.put(file.getPath(), file);
                }
                return false;
            }
        }
        return false;
    }

    public VirtualFile[] files() {
        return bucket.values().toArray(new VirtualFile[0]);
    }
}
