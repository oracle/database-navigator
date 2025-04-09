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

package com.dbn.data.grid.addon;

import com.dbn.common.addon.ComponentAddonBase;
import com.dbn.common.ui.util.Mouse;
import com.dbn.data.grid.ui.table.basic.BasicTable;
import com.dbn.data.model.DataModelCell;
import com.dbn.data.model.basic.BasicDataModel;
import com.dbn.data.preview.LargeValuePreviewPopup;
import com.dbn.data.value.LargeObjectValue;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;

import static com.dbn.common.ui.util.ClientProperty.VALUE_POPUP_ADDON;
import static java.awt.event.MouseEvent.BUTTON1;

public class ValuePopupAddon extends ComponentAddonBase<BasicTable> {

    private JBPopup valuePopup;

    public ValuePopupAddon(BasicTable component) {
        super(component);
        initSelectionListeners();
        Mouse.onMouseClick(component, BUTTON1, 1, e -> showCellValuePopup());
    }

    public BasicTable getTable() {
        return getComponent();
    }

    private void initSelectionListeners() {
        ListSelectionListener selectionListener = e -> {
            if (e.getValueIsAdjusting()) return;
            showCellValuePopup();
        };

        BasicTable table = getTable();
        table.getSelectionModel().addListSelectionListener(selectionListener);
        table.getColumnModel().getSelectionModel().addListSelectionListener(selectionListener);

        table.addPropertyChangeListener("columnModel", event -> {
            Object newValue = event.getNewValue();
            if (newValue instanceof TableColumnModel) {
                TableColumnModel columnModel = (TableColumnModel) newValue;
                columnModel.getSelectionModel().addListSelectionListener(selectionListener);
            }
        });

    }

    private void showCellValuePopup() {
        if (valuePopup != null) {
            valuePopup.cancel();
            valuePopup = null;
        }

        BasicTable table = getTable();
        if (!table.isLargeValuePopupActive()) return;
        if (table.isRestoringSelection()) return;
        if (!table.isShowing()) return;
        if (table.getSelectedRowCount() != 1) return;
        if (table.getSelectedColumnCount() != 1) return;

        BasicDataModel model = table.getModel();
        if (!model.isReadonly()) return;

        int rowIndex = table.getSelectedRow();
        int columnIndex = table.getSelectedColumn();
        if (canDisplayCompleteValue(rowIndex, columnIndex)) return;

        Rectangle cellRect = table.getCellRect(rowIndex, columnIndex, true);
        DataModelCell<?, ?> cell = (DataModelCell<?, ?>) table.getValueAt(rowIndex, columnIndex);
        TableColumn column = table.getColumnModel().getColumn(columnIndex);

        int preferredWidth = column.getWidth();
        LargeValuePreviewPopup viewer = new LargeValuePreviewPopup(table.getProject(), table, cell, preferredWidth);
        Point location = cellRect.getLocation();
        location.setLocation(location.getX() + 4, location.getY() + 20);

        valuePopup = viewer.show(table, location);
        valuePopup.addListener(
                new JBPopupListener() {
                    @Override
                    public void onClosed(@NotNull LightweightWindowEvent event) {
                        valuePopup.cancel();
                        valuePopup = null;
                    }
                }
        );
    }

    private boolean canDisplayCompleteValue(int rowIndex, int columnIndex) {
        BasicTable table = getTable();
        DataModelCell<?, ?> cell = (DataModelCell<?, ?>) table.getValueAt(rowIndex, columnIndex);
        if (cell == null) return true;

        Object value = cell.getUserValue();
        if (value == null) return true;
        if (value instanceof LargeObjectValue) return false;

        TableCellRenderer renderer = table.getCellRenderer(rowIndex, columnIndex);
        Component component = renderer.getTableCellRendererComponent(getTable(), cell, false, false, rowIndex, columnIndex);
        TableColumn column = table.getColumnModel().getColumn(columnIndex);
        return component.getPreferredSize().width <= column.getWidth();
    }

    public static void installTo(BasicTable table) {
        ValuePopupAddon addon = of(table);
        if (addon != null) return;

        addon = new ValuePopupAddon(table);
        VALUE_POPUP_ADDON.set(table, addon);
    }

    @Nullable
    public static ValuePopupAddon of(BasicTable table) {
        return VALUE_POPUP_ADDON.get(table);
    }

    public static void uninstallFrom(BasicTable table) {
        VALUE_POPUP_ADDON.set(table, null);
    }
}
