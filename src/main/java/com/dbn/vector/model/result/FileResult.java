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

import com.dbn.common.file.util.VirtualFiles;
import com.dbn.vector.model.request.EmbeddingSourceType;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.Arrays;

@Getter
@Setter
public class FileResult extends SourceResult {
  private final VirtualFile file;
  private final String size;
  private String fileStoreId;
  private String fileHash;
  private boolean isExisted = false;

  public FileResult(VirtualFile file) {
    super(EmbeddingSourceType.FILE_SYSTEM);
    this.file = file;
    this.size = VirtualFiles.getPresentableFileSize(file);
    initSteps();
  }

  private void initSteps() {
    steps = new ArrayList<>(Arrays.asList(
            new StepResult(PipelineStep.CHECK_CRC),
            new StepResult(PipelineStep.UPLOADING_FILE),
            new StepResult(PipelineStep.EMBED)
    ));
  }

  @NotNull
  @Override
  public String getName() {
    return file.getName();
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return file.getFileType().getIcon();
  }

  @Override
  public String getIdentifier() {
    return file.getPath();
  }
}
