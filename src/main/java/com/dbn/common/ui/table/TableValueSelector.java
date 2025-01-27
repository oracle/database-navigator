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

import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.Presentable;
import com.dbn.common.ui.util.Mouse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JTable;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.List;

import static com.dbn.common.ui.util.Popups.popupBuilder;

class TableValueSelector<T extends Presentable> {
    private final WeakRef<JTable> table;
    private final int columnIndex;
    private final String title;
    private final List<T> values;

    public TableValueSelector(JTable table, int columnIndex, String title, T[] values) {
        this.table = WeakRef.of(table);
        this.columnIndex = columnIndex;
        this.values = Arrays.asList(values);
        this.title = title;
        table.addKeyListener(createKeyListener());
        table.addMouseListener(createMouseListener());
    }

    @Nullable
    public JTable getTable() {
        return table.get();
    }

    private @NotNull KeyAdapter createKeyListener() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_SPACE) return;
                showValueSelector();
            }
        };
    }

    private MouseListener createMouseListener() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!Mouse.isMainSingleClick(e)) return;
                showValueSelector();
            }
        };
    }

    private void showValueSelector() {
        JTable table = getTable();
        if (table == null) return;

        int row = table.getSelectedRow();
        if (row < 0) return;

        int column = table.getSelectedColumn();
        if (column != columnIndex) return;

        Object value = table.getValueAt(row, column);

        popupBuilder(values, table, t -> table.setValueAt(t, row, column))
                .withTitle(title)
                .withTitleVisible(false)
                .withSpeedSearch(values.size() > 10)
                //.withPreselectCondition(e -> )
                .buildAndShow();
    }
}
