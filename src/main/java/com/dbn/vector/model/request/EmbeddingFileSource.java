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

package com.dbn.vector.model.request;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.io.InputStream;
import java.nio.file.Path;

@Data
@NoArgsConstructor
public class EmbeddingFileSource implements EmbeddingSource{
    private String filePath;
    private transient VirtualFile file;

    public EmbeddingFileSource(String filePath) {
        this.filePath = filePath;
    }

    public synchronized VirtualFile getFile() {
        if (file == null) {
            VirtualFileManager virtualFileManager = VirtualFileManager.getInstance();
            file = virtualFileManager.findFileByNioPath(Path.of(this.filePath));
        }
        return file;
    }

    @Override
    public String getIdentifier() {
        return filePath;
    }

    public Object getFileName() {
        return getFile().getName();
    }

    @SneakyThrows
    public InputStream getFileInputStream() {
        return getFile().getInputStream();
    }
}
