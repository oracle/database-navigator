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
  private VirtualFile file;
  private String size;
  private String docId;
  private boolean isExisted = false;

  public FileResult() {
    super(SourceType.FILE_SYSTEM);
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
}
