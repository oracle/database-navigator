package com.dbn.object;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface DBTable extends DBDataset, com.dbn.api.object.DBTable {
    boolean isTemporary();
    @Override
    @Nullable
    List<DBIndex> getIndexes();
    List<DBNestedTable> getNestedTables();
    @Override
    @Nullable
    DBIndex getIndex(String name);
    DBNestedTable getNestedTable(String name);
    List<DBColumn> getPrimaryKeyColumns();
    List<DBColumn> getForeignKeyColumns();
    List<DBColumn> getUniqueKeyColumns();
}