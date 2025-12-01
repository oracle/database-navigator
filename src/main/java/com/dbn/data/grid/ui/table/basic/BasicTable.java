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

package com.dbn.data.grid.ui.table.basic;

import com.dbn.common.color.Colors;
import com.dbn.common.event.ApplicationEvents;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.locale.options.RegionalSettings;
import com.dbn.common.locale.options.RegionalSettingsListener;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.table.DBNTableHeaderRenderer;
import com.dbn.common.ui.table.DBNTableWithGutter;
import com.dbn.common.ui.table.TableSelectionRestorer;
import com.dbn.common.util.MathResult;
import com.dbn.common.util.Safe;
import com.dbn.data.grid.addon.SelectionMathAddon;
import com.dbn.data.grid.addon.ValuePopupAddon;
import com.dbn.data.grid.color.DataGridTextAttributes;
import com.dbn.data.grid.options.DataGridSettings;
import com.dbn.data.model.ColumnInfo;
import com.dbn.data.model.DataModelCell;
import com.dbn.data.model.DataModelRow;
import com.dbn.data.model.basic.BasicDataModel;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.colors.EditorColorsListener;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Font;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;

@Getter
public class BasicTable<T extends BasicDataModel<?, ?>> extends DBNTableWithGutter<T> implements EditorColorsListener, Disposable {
    private final BasicTableCellRenderer cellRenderer;
    private final RegionalSettings regionalSettings;
    private final DataGridSettings dataGridSettings;
    private final TableSelectionRestorer selectionRestorer = createSelectionRestorer();

    public BasicTable(DBNComponent parent, T dataModel) {
        super(parent, dataModel, true);

        Project project = getProject();
        regionalSettings = RegionalSettings.getInstance(project);
        dataGridSettings = DataGridSettings.getInstance(project);
        cellRenderer = createCellRenderer();
        DataGridTextAttributes displayAttributes = cellRenderer.getAttributes();

        ApplicationEvents.subscribe(this, EditorColorsManager.TOPIC, this);
        Color bgColor = displayAttributes.getPlainData(false, false).getBgColor();
        setBackground(bgColor == null ? Colors.getTableBackground() : bgColor);

        addPropertyChangeListener(e -> {
            Object newProperty = e.getNewValue();
            if (newProperty instanceof Font font) {
                adjustRowHeight();
                JTableHeader tableHeader = getTableHeader();
                if (tableHeader != null) {
                    TableCellRenderer defaultRenderer = tableHeader.getDefaultRenderer();
                    if (defaultRenderer instanceof DBNTableHeaderRenderer renderer) {
                        renderer.setFont(font);
                    }
                }
                adjustColumnWidths();
            }

        });

        ProjectEvents.subscribe(project, this, RegionalSettingsListener.TOPIC, regionalSettingsListener);
        ApplicationEvents.subscribe(this, EditorColorsManager.TOPIC, this);

        //EventUtil.subscribe(this, UISettingsListener.TOPIC, this);
    }

    public void installMathAddon() {
        SelectionMathAddon.installTo(this);
    }

    public void installValuePopupAddon() {
        ValuePopupAddon.installTo(this);
    }

    private final RegionalSettingsListener regionalSettingsListener = () -> regionalSettingsChanged();

    protected void regionalSettingsChanged() {
        resizeAndRepaint();
    }

    @NotNull
    public BasicTableSelectionRestorer createSelectionRestorer() {
        return new BasicTableSelectionRestorer();
    }

    public boolean isRestoringSelection() {
        return selectionRestorer.isRestoring();
    }

    public void snapshotSelection() {
        selectionRestorer.snapshot();
    }

    public void restoreSelection() {
        selectionRestorer.restore();
    }

    @Override
    protected BasicTableGutter<?> createTableGutter() {
        return new BasicTableGutter<>(this);
    }

    protected BasicTableCellRenderer createCellRenderer() {
        return new BasicTableCellRenderer();
    }

    public void selectRow(int index) {
        T model = getModel();
        int rowCount = model.getRowCount();
        int columnCount = model.getColumnCount();

        if (rowCount <= index) return;
        if (columnCount <= 0) return;

        clearSelection();
        int lastColumnIndex = Math.max(0, columnCount - 1);
        setColumnSelectionInterval(0, lastColumnIndex);
        getSelectionModel().setSelectionInterval(index, index);
        Safe.run(getTableGutter(), g -> g.setSelectedIndex(index));

        scrollRectToVisible(getCellRect(index, 0, true));
    }

    protected ColumnInfo getColumnInfo(int columnIndex) {
        return getModel().getColumnInfo(columnIndex);
    }

    @Override
    public TableCellRenderer getCellRenderer(int i, int i1) {
        return cellRenderer;
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        super.tableChanged(e);
        int firstRow = e.getFirstRow();
        int lastRow = e.getLastRow();
        if (firstRow == -1 && lastRow == -1) return;

        if (firstRow != lastRow) {
            adjustColumnWidths();
        }
    }

    @Nullable
    public DataModelCell<?, ?> getCellAtLocation(Point point) {
        int columnIndex = columnAtPoint(point);
        int rowIndex = rowAtPoint(point);
        return columnIndex > -1 && rowIndex > -1 ? getCellAtPosition(rowIndex, columnIndex) : null;
    }

    @Nullable
    protected DataModelCell<?, ?> getCellAtMouseLocation() {
        Point location = MouseInfo.getPointerInfo().getLocation();
        location.setLocation(location.getX() - getLocationOnScreen().getX(), location.getY() - getLocationOnScreen().getY());
        return getCellAtLocation(location);
    }

    @Nullable
    protected DataModelCell<?, ?> getCellAtPosition(int modelRowIndex, int modelColumnIndex) {
        DataModelRow<?, ?> row = getModel().getRowAtIndex(modelRowIndex);
        if (row == null) return null;

        return row.getCellAtIndex(modelColumnIndex);
    }
    /*********************************************************
     *                EditorColorsListener                  *
     *********************************************************/
    @Override
    public void globalSchemeChange(EditorColorsScheme scheme) {
        updateBackground(isLoading());
        resizeAndRepaint();
/*        JBScrollPane scrollPane = UIUtil.getParentOfType(JBScrollPane.class, this);
        if (scrollPane != null) {
            scrollPane.revalidate();
            scrollPane.repaint();
        }*/
    }

    /*********************************************************
     *                ListSelectionListener                  *
     *********************************************************/
    @Override
    public void valueChanged(ListSelectionEvent e) {
        super.valueChanged(e);
        if (e.getValueIsAdjusting()) return;
        if (!hasFocus()) return;

        clearGutterSelection();
    }

    @Override
    public void columnSelectionChanged(ListSelectionEvent e) {
        JTableHeader tableHeader = getTableHeader();
        if (tableHeader != null && tableHeader.getDraggedColumn() == null) {
            super.columnSelectionChanged(e);
        }
    }

    public boolean isLargeValuePopupActive() {
        return true;
    }

    @Nullable
    public MathResult getSelectionMath() {
        SelectionMathAddon mathAddon = SelectionMathAddon.of(this);
        return mathAddon == null ? null : mathAddon.getMathResult();
    }

    public Rectangle getCellRect(DataModelCell<?, ?> cell) {
        int rowIndex = convertRowIndexToView(cell.getRow().getIndex());
        int columnIndex = convertColumnIndexToView(cell.getIndex());
        return getCellRect(rowIndex, columnIndex, true);
    }

    @NotNull
    @Override
    public T getModel() {
        return super.getModel();
    }

}
