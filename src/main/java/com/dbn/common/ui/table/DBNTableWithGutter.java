/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.common.ui.table;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.ui.component.DBNComponent;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JScrollPane;
import javax.swing.event.TableModelEvent;

import static com.dbn.common.ui.util.UserInterface.whenFirstShown;

@Getter
public class DBNTableWithGutter<T extends DBNTableWithGutterModel> extends DBNTable<T>{
    public DBNTableWithGutter(DBNComponent parent, T tableModel, boolean showHeader) {
        super(parent, tableModel, showHeader);
        whenFirstShown(this, () -> initTableGutter());
    }

    private DBNTableGutter<?> tableGutter;


    public void tableChanged(TableModelEvent e) {
        super.tableChanged(e);
        refreshTableGutter();
    }

    public boolean isGutterFocussed() {
        return tableGutter != null && tableGutter.hasFocus();
    }

    protected DBNTableGutter<?> createTableGutter() {
        return null; // do not create gutter by default
    }

    public void clearGutterSelection() {
        if (tableGutter == null) return;
        tableGutter.clearSelection();
    }

    public final void initTableGutter() {
        JScrollPane scrollPane = getScrollPane();
        if (scrollPane == null) return;

        tableGutter = createTableGutter();
        if (tableGutter == null) return;

        scrollPane.setRowHeaderView(tableGutter);
        tableGutter.adjustCellSize();
    }

    public void refreshTableGutter() {
        DBNTableGutter tableGutter = this.tableGutter;
        if (tableGutter == null) return;

        JScrollPane scrollPane = getScrollPane();
        if (scrollPane == null) return;

        // scrolling glitch if gutter model size changes
        this.tableGutter = createTableGutter();
        initTableGutter();

        Disposer.dispose(tableGutter);
    }

    private @Nullable JScrollPane getScrollPane() {
        return UIUtil.getParentOfType(JScrollPane.class, this);
    }

    @NotNull
    @Override
    public T getModel() {
        return super.getModel();
    }
}
