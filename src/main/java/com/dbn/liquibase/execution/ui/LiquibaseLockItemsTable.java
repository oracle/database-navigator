/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.dbn.liquibase.execution.ui;

import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.table.DBNDynamicTableCellRenderer;
import com.dbn.common.ui.table.DBNTableGutter;
import com.dbn.common.ui.table.DBNTableTransferHandler;
import com.dbn.common.ui.table.DBNTableWithGutter;
import com.dbn.common.ui.util.Accessibility;
import com.dbn.common.ui.util.Borderless;
import org.jetbrains.annotations.NotNull;

import javax.swing.table.TableModel;

import static com.dbn.nls.NlsResources.txt;

/** Table displaying Liquibase changelog locks. */
public class LiquibaseLockItemsTable extends DBNTableWithGutter<LiquibaseLockItemsTableModel> {
    public LiquibaseLockItemsTable(
            @NotNull DBNComponent parent,
            LiquibaseLockItemsTableModel model) {
        super(parent, model, true);
        setCellSelectionEnabled(true);
        setDefaultRenderer(Object.class, new DBNDynamicTableCellRenderer());
        setTransferHandler(DBNTableTransferHandler.INSTANCE);
        setProportionalColumnWidth(model.getColumnCount() - 1, 30);
        initTableSorter();
        Borderless.markBorderless(this);
        Accessibility.setAccessibleName(this, txt("app.liquibase.aria.LockItems"));
    }

    @Override
    protected DBNTableGutter<?> createTableGutter() {
        return new DBNTableGutter<>(this);
    }

    @Override
    public void setModel(@NotNull TableModel dataModel) {
        super.setModel(dataModel);
        initTableSorter();
    }
}
