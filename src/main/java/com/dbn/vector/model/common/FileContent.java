package com.dbn.vector.model.common;

import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Getter
@Setter
public class FileContent {
  private static final int BUFFER_SIZE = 64 * 1024;

  private final VirtualFile file;
  private final byte[] fileBytes;
  private final String md5Hash;
  private final long fileSize;
  private final long computedAt;
  private Map<String, Object> metadata;

  public FileContent(@NotNull VirtualFile file) throws IOException, NoSuchAlgorithmException {
    this.file = file;
    this.fileSize = file.getLength();

    // READ FILE ONCE - entire content into memory
    this.fileBytes = readFileToBytes(file);

    this.md5Hash = computeMD5FromBytes(this.fileBytes);

    this.computedAt = System.currentTimeMillis();
  }

  private byte[] readFileToBytes(@NotNull VirtualFile file) throws IOException {
    if (file.getLength() > 2_000_000_000L) { // 2GB limit
      throw new IOException(
              String.format("File too large: %d bytes. Maximum supported: 2GB", file.getLength())
      );
    }

    byte[] result = new byte[(int) file.getLength()];
    int totalRead = 0;

    try (InputStream in = file.getInputStream()) {
      byte[] buffer = new byte[BUFFER_SIZE];
      int bytesRead;

      while ((bytesRead = in.read(buffer)) != -1) {
        System.arraycopy(buffer, 0, result, totalRead, bytesRead);
        totalRead += bytesRead;
      }
    }

    if (totalRead != result.length) {
      throw new IOException(
              String.format("File read incomplete: expected %d bytes, got %d",
                      result.length, totalRead)
      );
    }

    return result;
  }


  private String computeMD5FromBytes(@NotNull byte[] bytes) throws NoSuchAlgorithmException {
    MessageDigest md5 = MessageDigest.getInstance("MD5");
    md5.update(bytes);
    return bytesToHex(md5.digest());
  }


  private String bytesToHex(byte[] bytes) {
    StringBuilder result = new StringBuilder();
    for (byte b : bytes) {
      result.append(String.format("%02x", b));
    }
    return result.toString();
  }

  public String getHash(){
    return md5Hash;
  }

  public byte[] getBytes() {
    return fileBytes;
  }
}
