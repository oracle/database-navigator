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

import lombok.Getter;

public enum PipelineStep {
  ENSURE_DESTINATION(true,
          "Prepare Vector Table",
          "Create or verify the destination vector table used to store chunk embeddings."),
  ENSURE_DOCUMENT_TABLE(true,
          "Prepare Document Store",
          "Create or verify the document table used to persist original files (LOBs) and metadata."),
  CHECK_CRC(false,
          "Check for Duplicated File",
          "Compute and look up the file checksum to detect previously uploaded files. If a match is found, " +
                  "the file is skipped to prevent duplicates."),
  UPLOADING_FILE(false,
          "Upload to Database",
          "Stream the file content into the document store and record file metadata."),
  EMBED(false,
          "Chunk & Embed Content",
          "Extract text, split into chunks according to the configured chunker, request embeddings from the "+
                  "chosen model/provider, and insert vector rows into the destination table.");
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
