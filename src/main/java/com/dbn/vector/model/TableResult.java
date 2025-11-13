package com.dbn.vector.model;

import com.dbn.connection.ConnectionId;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.dbn.vector.model.sourceconfig.SourceType;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

// TableResult for table-based jobs
@Getter
@Setter
public class TableResult extends SourceResult {
  private final DBObjectRef<DBTable> table;
  private long rowsScanned = 0;      // number of source rows visited
  private int batchSize = 0;         // batch size used for embedding
  private long rowsFailed = 0;
  private String firstKey = null;    // optional checkpoint keys
  private String lastKey = null;

  public TableResult(ConnectionId connectionId, String schemaName, String tableName) {
    super(SourceType.DATABASE_TABLE);
    DBObjectRef<DBSchema> schema = new DBObjectRef<>(connectionId, DBObjectType.SCHEMA, schemaName);
    table = new DBObjectRef<>(schema, DBObjectType.TABLE, tableName);
  }

  @NotNull
  @Override
  public String getName() {
    return table.getQualifiedName();
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return DBObjectType.TABLE.getIcon();
  }

  @Override
  public String getSize() {
    return ""; // TODO display "x rows" (select count(1) from table)
  }

  @Override
  public String getIdentifier() {
    return table.getQualifiedName();
  }
}
