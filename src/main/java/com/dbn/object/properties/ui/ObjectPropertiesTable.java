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

import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.table.DBNColoredTableCellRenderer;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.ui.table.DBNTableModel;
import com.dbn.common.ui.util.Borderless;
import com.dbn.common.ui.util.Cursors;
import com.dbn.common.ui.util.Keyboard.Key;
import com.dbn.common.ui.util.Mouse;
import com.dbn.object.properties.PresentableProperty;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.Nullable;

import javax.swing.table.TableCellRenderer;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;

public class ObjectPropertiesTable extends DBNTable<DBNTableModel> implements Borderless{
    ObjectPropertiesTable(DBNForm parent, DBNTableModel tableModel) {
        super(parent, tableModel, false);
        setDefaultRenderer(String.class, cellRenderer);
        setDefaultRenderer(PresentableProperty.class, cellRenderer);
        setCellSelectionEnabled(true);

        addMouseListener(createMouseListener());
        addKeyListener(createKeyListener());

        setAccessibleName(this, "Object Properties");
    }

    private MouseListener createMouseListener() {
        return Mouse.listener().onClick(e -> {
            if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() > 1) {
                navigateInBrowser();
                e.consume();
            }


            if (Mouse.isNavigationEvent(e)) {
                navigateInBrowser();
                e.consume();
            }
        });
    }

    private KeyListener createKeyListener() {
        return new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == Key.ENTER) {
                    navigateInBrowser();
                }
            }
        };
    }


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

    private final TableCellRenderer cellRenderer = new DBNColoredTableCellRenderer() {
        @Override
        protected void customizeCellRenderer(DBNTable table, @Nullable Object value, boolean selected, boolean hasFocus, int row, int column) {
            PresentableProperty property = (PresentableProperty) value;
            if (property == null) return;

            if (column == 0) {
                setIcon(null);
                append(property.getName());
            } else if (column == 1) {
                append(property.getValue());
                setIcon(property.getIcon());
            }
        }
    };
}
