package com.dbn.object;

public interface DBView extends DBDataset, com.dbn.api.object.DBView {
    DBType getType();

    boolean isSystemView();
}