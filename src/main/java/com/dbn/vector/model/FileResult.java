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
  private VirtualFile file; // todo make final
  private String size;      // todo make final
  private String docId;
  private boolean isExisted = false;
  private transient java.sql.Blob cachedBlob; // Store blob when retrieved during CRC check

  @Deprecated // TODO initialize file result with the file using below constructor
  public FileResult() {
    super(SourceType.FILE_SYSTEM);
    initSteps();
  }

  private void initSteps() {
    steps = new ArrayList<>(Arrays.asList(
            new StepResult(PipelineStep.CHECK_CRC),
            new StepResult(PipelineStep.UPLOADING_FILE),
            new StepResult(PipelineStep.EMBED)
    ));
  }


  public FileResult(VirtualFile file) {
    super(SourceType.FILE_SYSTEM);
    this.file = file;
    this.size = VirtualFiles.getPresentableFileSize(file);
    initSteps();
  }

  public void setFile(VirtualFile file) {
    this.file = file;
    this.size = VirtualFiles.getPresentableFileSize(file);
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
