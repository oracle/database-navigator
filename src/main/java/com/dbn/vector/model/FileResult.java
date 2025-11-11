package com.dbn.vector.model;

import com.dbn.vector.model.sourceconfig.SourceType;
import lombok.Setter;

// FileResult now extends SourceResult
@Setter
public  class FileResult extends SourceResult{
  private  String filename;
  private  String docId;
  private boolean isExisted = false;

  public FileResult() {
    super(SourceType.FILE_SYSTEM);
  }

  public void setFilename(String filename) {
    this.filename = filename;
    setDisplayName(filename);
  }
}
