package com.dbn.vector.model;

import com.dbn.common.file.util.VirtualFiles;
import com.dbn.vector.model.sourceconfig.SourceType;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

// FileResult now extends SourceResult
@Getter
@Setter
public class FileResult extends SourceResult{
  private VirtualFile file; // todo make final
  private String size;      // todo make final
  private String docId;
  private boolean isExisted = false;

  @Deprecated // TODO initialize file result with the file using below constructor
  public FileResult() {
    super(SourceType.FILE_SYSTEM);
  }

  public FileResult(VirtualFile file) {
    super(SourceType.FILE_SYSTEM);
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
