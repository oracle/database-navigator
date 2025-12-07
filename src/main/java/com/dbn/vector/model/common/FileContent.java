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
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Getter
@Setter
public class FileContent {
  private static final int BUFFER_SIZE = 64 * 1024;

  private final VirtualFile file;
  private final String md5Hash;
  private final long fileSize;
  private final long computedAt;
  private Map<String, Object> metadata;
  private String id;

  public FileContent(@NotNull VirtualFile file) throws IOException, NoSuchAlgorithmException {
    this.file = file;
    this.fileSize = file.getLength();


    this.md5Hash = Checksum.fromFileContent( VfsUtilCore.virtualToIoFile(file), ChecksumType.MD_5);

    this.computedAt = System.currentTimeMillis();
  }



  public String getHash(){
    return md5Hash;
  }

  public InputStream getInputStream() throws IOException {
    return file.getInputStream();
  }


}
