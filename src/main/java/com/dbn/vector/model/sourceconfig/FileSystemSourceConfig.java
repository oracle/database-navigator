package com.dbn.vector.model.sourceconfig;

import com.dbn.vector.model.common.CreateTableConfig;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Setter
@Getter
public class FileSystemSourceConfig extends SourceConfig {
  private List<String> filePaths;

  private boolean store;
  // if it's to be stored
  private CreateTableConfig tableConfig;

  public List<VirtualFile> getFiles() {
    if (filePaths == null) return emptyList();
    VirtualFileManager fileManager = VirtualFileManager.getInstance();
    return filePaths
            .stream()
            .map(p -> fileManager.findFileByNioPath(Path.of(p)))
            .filter(f -> f != null)
            .collect(Collectors.toList());
  }
}
