package com.dbn.vector.model.common;

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
