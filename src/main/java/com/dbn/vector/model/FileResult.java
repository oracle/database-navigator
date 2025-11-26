package com.dbn.vector.model;

import com.dbn.common.file.util.VirtualFiles;
import com.dbn.vector.model.sourceconfig.SourceType;
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
public class FileResult extends SourceResult{
  private final VirtualFile file;
  private final String size;
  private String docId;
  private boolean isExisted = false;

  public FileResult(VirtualFile file) {
    super(SourceType.FILE_SYSTEM);
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
