package com.dbn.vector.model.common;

import org.jetbrains.annotations.Nullable;

public class DuplicateInfo {
  public final boolean exists;
  public final String existingDocId;

  public DuplicateInfo(boolean exists, @Nullable String existingDocId) {
    this.exists = exists;
    this.existingDocId = existingDocId;
  }

  public static DuplicateInfo notFound() {
    return new DuplicateInfo(false, null);
  }

  public static DuplicateInfo found(String docId) {
    return new DuplicateInfo(true, docId);
  }
}
