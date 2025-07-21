package com.dbn.vector.model.sourceconfig;

import com.dbn.object.DBColumn;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class DBTableSourceConfig extends SourceConfig {
  private DBSchema sourceSchema;
  private DBTable sourceTable;
  private DBColumn dataColumn;
  private DBColumn idColumn;
  private boolean isAutoSync;
}
