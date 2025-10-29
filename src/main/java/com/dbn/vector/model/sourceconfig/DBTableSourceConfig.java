package com.dbn.vector.model.sourceconfig;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class DBTableSourceConfig extends SourceConfig {
  private String schemaName;
  private String tableName;
  private String keyColumnName;
  private String dataColumnName;
  private boolean autoSync;
}
