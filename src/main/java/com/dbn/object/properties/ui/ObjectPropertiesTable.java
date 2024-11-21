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

package com.dbn.object.properties.ui;

import com.dbn.common.dispose.Failsafe;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.ui.table.DBNTableModel;
import com.dbn.common.ui.util.Borderless;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Cursors;
import com.dbn.common.ui.util.Keyboard.Key;
import com.dbn.common.ui.util.Mouse;
import com.dbn.object.properties.PresentableProperty;
import com.intellij.pom.Navigatable;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class ObjectPropertiesTable extends DBNTable<DBNTableModel> implements Borderless{
    ObjectPropertiesTable(DBNForm parent, DBNTableModel tableModel) {
        super(parent, tableModel, false);
        setDefaultRenderer(String.class, cellRenderer);
        setDefaultRenderer(PresentableProperty.class, cellRenderer);
        setCellSelectionEnabled(true);
        adjustRowHeight(3);

        addMouseListener(mouseListener);
        addKeyListener(keyListener);
    }

    private final MouseListener mouseListener = Mouse.listener().onClick(e -> {
        if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() > 1) {
            navigateInBrowser();
            e.consume();
        }


        if (Mouse.isNavigationEvent(e)) {
            navigateInBrowser();
            e.consume();
        }
    });


    private final KeyListener keyListener = new KeyAdapter() {
        @Override
        public void keyTyped(KeyEvent e) {
            if (e.getKeyChar() == Key.ENTER) {
                navigateInBrowser();
            }
        }
    };


    private void navigateInBrowser() {
        int rowIndex = getSelectedRow();
        int columnIndex = getSelectedColumn();
        if (columnIndex == 1) {
            PresentableProperty presentableProperty = (PresentableProperty) getModel().getValueAt(rowIndex, 1);
            Navigatable navigatable = presentableProperty.getNavigatable();
            if (navigatable != null) navigatable.navigate(true);
        }
    }


    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        if (e.isControlDown() && e.getID() != MouseEvent.MOUSE_DRAGGED && isNavigableCellAtMousePosition()) {
            setCursor(Cursors.handCursor());
        } else {
            super.processMouseMotionEvent(e);
            setCursor(Cursors.defaultCursor());
        }
    }

    private boolean isNavigableCellAtMousePosition() {
        Object value = getValueAtMouseLocation();
        if (value instanceof PresentableProperty) {
            PresentableProperty property = (PresentableProperty) value;
            return property.getNavigatable() != null;
        }
        return false;
    }

    private final TableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            return Failsafe.guarded(component, () -> {
                PresentableProperty property = (PresentableProperty) value;
                if (property != null) {
                    if (column == 0) {
                        setIcon(null);
                        setText(property.getName());
                        //setFont(GUIUtil.BOLD_FONT);
                    } else if (column == 1) {
                        setText(property.getValue());
                        setIcon(property.getIcon());
                        //setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        //setFont(property.getIcon() == null ? GUIUtil.BOLD_FONT : GUIUtil.REGULAR_FONT);
                    }
                }

                Dimension dimension = getSize();
                dimension.setSize(dimension.getWidth(), 30);
                setSize(dimension);
                setBorder(Borders.TEXT_FIELD_INSETS);

                return component;
            });
        }

    };
}
