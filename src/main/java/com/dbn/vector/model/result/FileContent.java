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

package com.dbn.vector.model.result;

import com.dbn.common.checksum.Checksum;
import com.dbn.common.checksum.ChecksumType;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Getter
@Setter
public class FileContent {
  private final VirtualFile file;
  private final String fileHash;
  private final long fileSize;
  private final long computedAt;

  private String fileStoreId;
  private Map<String, Object> metadata;

  public FileContent(@NotNull VirtualFile file) {
    this.file = file;
    this.fileSize = file.getLength();


    this.fileHash = Checksum.fromFileContent(VfsUtilCore.virtualToIoFile(file), ChecksumType.SHA_256);
    this.computedAt = System.currentTimeMillis();
  }

  public InputStream getInputStream() throws IOException {
    return file.getInputStream();
  }


}
