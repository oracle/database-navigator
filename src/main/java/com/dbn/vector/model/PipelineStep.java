package com.dbn.vector.model;

import lombok.Getter;

import javax.swing.*;

public enum PipelineStep {
  ENSURE_DESTINATION(true,"Prepare Vector Table","Create or verify the destination vector table used to store chunk embeddings"),
  ENSURE_DOCUMENT_TABLE(true,"Prepare Documents Store","Create or verify the documents table used to persist original files (LOBs) and metadata before embedding."),
  CHECK_CRC(false,"Check for Duplicate File",    "Compute and look up the file checksum to detect previously uploaded files. If a match is found, the file is skipped to prevent duplicate uploads."),
  UPLOADING_FILE(false,"Upload to Database","Stream the file content into the document store (SecureFile LOB) and record file metadata"),
  EMBED(false,"Chunk & Embed Content","Extract text, split into chunks according to the configured chunker, request embeddings from the chosen model/provider, and insert vector rows into the destination table.");
  @Getter
  private final String displayName;
  @Getter
  private final String description;
  @Getter
  private final boolean critical;

  PipelineStep(boolean critical, String displayName, String description) {
    this.critical = critical;
    this.displayName = displayName;
    this.description = description;
  }

}
