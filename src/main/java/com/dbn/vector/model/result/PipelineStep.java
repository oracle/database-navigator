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

@Getter
public enum PipelineStep {
  CHECK_CRC(
          "Check for Duplicated File",
          "Compute and look up the file checksum to detect previously uploaded files. If a match is found, " +
                  "the file is skipped to prevent duplicates."),
  UPLOADING_FILE(
          "Upload to Database",
          "Stream the file content into the document store and record file metadata."),
  EMBED(
          "Chunk & Embed Content",
          "Extract text, split into chunks according to the configured chunker, request embeddings from the "+
                  "chosen model/provider, and insert vector rows into the destination table.");
  private final String displayName;
  private final String description;

  PipelineStep(String displayName, String description) {
    this.displayName = displayName;
    this.description = description;
  }

}
