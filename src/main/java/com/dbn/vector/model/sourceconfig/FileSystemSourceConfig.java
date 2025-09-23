package com.dbn.vector.model.sourceconfig;

import com.dbn.vector.model.common.CreateTableConfig;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Setter
@Getter
public class FileSystemSourceConfig extends SourceConfig {
  private List<VirtualFile> virtualFiles;
  private boolean isToStore;
  // if it's to be stored
  private CreateTableConfig tableConfig;
}
