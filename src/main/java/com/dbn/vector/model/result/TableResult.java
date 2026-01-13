/*
 * Copyright 2025 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.vector.model.result;

import com.dbn.connection.ConnectionId;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.dbn.vector.model.request.EmbeddingSourceType;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.Arrays;

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
    super(EmbeddingSourceType.DATABASE_TABLE);
    DBObjectRef<DBSchema> schema = new DBObjectRef<>(connectionId, DBObjectType.SCHEMA, schemaName);
    table = new DBObjectRef<>(schema, DBObjectType.TABLE, tableName);
    initSteps();
  }

  private void initSteps() {
    steps = new ArrayList<>(Arrays.asList(
            new StepResult(PipelineStep.EMBED)
    ));
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
