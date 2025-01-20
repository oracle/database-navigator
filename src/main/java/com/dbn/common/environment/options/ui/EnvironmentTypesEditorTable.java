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

package com.dbn.common.environment.options.ui;

import com.dbn.common.environment.EnvironmentTypeBundle;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.table.DBNEditableTable;
import com.dbn.common.ui.util.Cursors;
import com.dbn.common.ui.util.Mouse;
import com.intellij.openapi.project.Project;
import com.intellij.ui.BooleanTableCellEditor;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.ui.ColorChooser;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.ListSelectionModel;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.ui.util.Mouse.isMainSingleClick;

public class EnvironmentTypesEditorTable extends DBNEditableTable<EnvironmentTypesTableModel> {

    EnvironmentTypesEditorTable(DBNComponent parent, EnvironmentTypeBundle environmentTypes) {
        super(parent, createModel(parent.getProject(), environmentTypes), true);
        setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        setSelectionBackground(UIUtil.getTableBackground());
        setSelectionForeground(UIUtil.getTableForeground());
        setCellSelectionEnabled(true);
        setDefaultRenderer(String.class, new EnvironmentTypesTableCellRenderer());
        setDefaultRenderer(Color.class, new EnvironmentTypesTableCellRenderer());
        setDefaultRenderer(Boolean.class, new BooleanTableCellRenderer());
        setDefaultEditor(Boolean.class, new BooleanTableCellEditor());

        setFixedColumnWidths(-1, -1, 120, 120, 60);
        setProportionalColumnWidths(25, 40);
        addMouseListener(createMouseListener());
        addKeyListener(createKeyListener());

        setAccessibleName(this, "Environment Types");
    }

    private KeyListener createKeyListener() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_SPACE) return;

                int columnIndex = getSelectedColumn();
                if (columnIndex != 4) return;

                int rowIndex = getSelectedRow();
                if (rowIndex == -1) return;

                openColorChooser(rowIndex, columnIndex);
            }
        };
    }

    @NotNull
    private static EnvironmentTypesTableModel createModel(Project project, EnvironmentTypeBundle environmentTypes) {
        return new EnvironmentTypesTableModel(project, environmentTypes);
    }

    void setEnvironmentTypes(EnvironmentTypeBundle environmentTypes) {
        super.setModel(createModel(getProject(), environmentTypes));
    }

    MouseListener createMouseListener() {
        return Mouse.listener().onClick(e -> {
            if (!isMainSingleClick(e)) return;

            Point point = e.getPoint();
            int columnIndex = columnAtPoint(point);
            if (columnIndex != 4) return;

            int rowIndex = rowAtPoint(point);
            openColorChooser(rowIndex, columnIndex);
        });
    }

    private void openColorChooser(int rowIndex, int columnIndex) {
        Color color = (Color) getValueAt(rowIndex, columnIndex);
        color = ColorChooser.chooseColor(EnvironmentTypesEditorTable.this, "Select Environment Color", color);
        if (color == null) return;

        setValueAt(color, rowIndex, columnIndex);
    }


    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        Object value = getValueAtMouseLocation();
        if (value instanceof Color) {
            setCursor(Cursors.handCursor());
        } else {
            setCursor(Cursors.defaultCursor());
        }
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return column < 4;
    }
}
