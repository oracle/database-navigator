/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.dbn.liquibase.execution.ui;

import com.dbn.common.ui.table.DBNDynamicTableModel;
import com.dbn.liquibase.execution.LiquibaseLockItem;
import com.dbn.liquibase.operation.LiquibaseOperationResult;
import lombok.Getter;

import static com.dbn.nls.NlsResources.txt;

/** Table model for Liquibase changelog locks. */
@Getter
public class LiquibaseLockItemsTableModel extends DBNDynamicTableModel<LiquibaseLockItem> {
    private final LiquibaseOperationResult result;

    public LiquibaseLockItemsTableModel(LiquibaseOperationResult result) {
        super(LiquibaseLockItem.class, result.getLockItems());
        this.result = result;
        addColumn(txt("app.liquibase.column.DiscoveryOrder"), e -> getData().indexOf(e) + 1);
        addColumn(txt("app.liquibase.column.LockId"), e -> e.getId());
        addColumn(txt("app.liquibase.column.LockedBy"), e -> e.getLockedBy());
        addColumn(txt("app.liquibase.column.LockGranted"), e -> e.getLockGranted());
    }

    public void refresh() {
        setData(result.getLockItems());
    }
}
