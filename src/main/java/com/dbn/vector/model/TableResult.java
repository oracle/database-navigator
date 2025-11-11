package com.dbn.vector.model;

import com.dbn.vector.model.sourceconfig.SourceType;

// TableResult for table-based jobs
public  class TableResult extends SourceResult {
  private final String tableName;
  private long rowsScanned = 0;      // number of source rows visited
  private int batchSize = 0;         // batch size used for embedding
  private long rowsFailed = 0;
  private String firstKey = null;    // optional checkpoint keys
  private String lastKey = null;

  public TableResult(String tableName) {
    super(SourceType.DATABASE_TABLE);
    setDisplayName(tableName);
    this.tableName = tableName;
  }
}
