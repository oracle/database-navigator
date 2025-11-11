package com.dbn.vector.model;

public enum PipelineStep {
  ENSURE_DESTINATION(true,"Prepare Vector Table"),
  ENSURE_DOCUMENT_TABLE(true,"Prepare Documents Store"),
  CHECK_CRC(false,"Check for Duplicate File"),
  UPLOADING_FILE(false,"Upload to Database"),
  EMBED(false,"Chunk & Embed Content"),
  CLEANUP(false,"Cleanup"),;
  private String displayName;
  private final boolean critical;
  PipelineStep(boolean critical,String displayName) {
    this.critical = critical;
    this.displayName = displayName;
  }
  public String getDisplayName() {
    return displayName;
  }
  public boolean isCritical() { return critical; }
}
