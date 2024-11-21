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

import com.dbn.common.color.Colors;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.latent.Latent;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.FontMetrics;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Cursors;
import com.dbn.common.ui.util.Mouse;
import com.dbn.common.util.Strings;
import com.dbn.data.grid.ui.table.basic.BasicTableHeaderRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.util.keyFMap.KeyFMap;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.event.EventListenerList;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.dbn.common.dispose.ComponentDisposer.removeListeners;
import static com.dbn.common.dispose.Disposer.replace;
import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class DBNTable<T extends DBNTableModel> extends JTable implements StatefulDisposable, UserDataHolder {
    private static final int MAX_COLUMN_WIDTH = 300;
    private static final int MIN_COLUMN_WIDTH = 10;

    private final WeakRef<DBNComponent> parentComponent;

    private int rowVerticalPadding;
    private double scrollDistance;
    private KeyFMap userData = KeyFMap.EMPTY_MAP;

    private Timer scrollTimer;
    private final Latent<DBNTableGutter<?>> tableGutter = Latent.weak(() -> createTableGutter());
    private final FontMetrics metricsCache = new FontMetrics(this);

    public DBNTable(DBNComponent parent, T tableModel, boolean showHeader) {
        super(nd(tableModel));
        this.parentComponent = WeakRef.of(nd(parent));

        setGridColor(Colors.getTableGridColor());
        Font font = getFont();//UIUtil.getListFont();
        setFont(font);
        setBackground(Colors.getListBackground());
        setTransferHandler(DBNTableTransferHandler.INSTANCE);

        adjustRowHeight(1);

        JTableHeader tableHeader = getTableHeader();
        if (!showHeader) {
            tableHeader.setVisible(false);
            tableHeader.setPreferredSize(new Dimension(-1, 0));
        } else {
            tableHeader.setBackground(Colors.getPanelBackground());
            tableHeader.setBorder(Borders.tableBorder(0, 0, 1, 0));
            tableHeader.setDefaultRenderer(new BasicTableHeaderRenderer());
            tableHeader.addMouseMotionListener(Mouse.listener().onDrag(e -> {
                JScrollPane scrollPane = getScrollPane();
                if (scrollPane == null) return;

                calculateScrollDistance();
                if (scrollDistance != 0 && scrollTimer == null) {
                    scrollTimer = new Timer();
                    scrollTimer.schedule(new ScrollTask(), 100, 100);
                }
            }));

            tableHeader.addMouseListener(Mouse.listener().onRelease(e -> {
                if (scrollTimer == null) return;

                Disposer.dispose(scrollTimer);
                scrollTimer = null;
            }));
        }

        setSelectionBackground(Colors.getTableSelectionBackground(true));
        setSelectionForeground(Colors.getTableSelectionForeground(true));

        Disposer.register(parent, this);
        Disposer.register(this, tableModel);
    }

    @Nullable
    public JViewport getViewport() {
        return UIUtil.getParentOfType(JViewport.class, this);
    }

    @Nullable
    public JScrollPane getScrollPane() {
        return UIUtil.getParentOfType(JScrollPane.class, this);
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        JViewport viewport = getViewport();
        if (viewport == null) return;
        viewport.setBackground(bg);
    }

    @Override
    public String getToolTipText(@NotNull MouseEvent e) {
        return null;
    }

    @Override
    public void setModel(@NotNull TableModel dataModel) {
        dataModel = replace(super.getModel(), dataModel);
        super.setModel(dataModel);
    }

    protected void initTableSorter() {
        setRowSorter(new DBNTableSorter(getModel()));
        JTableHeader tableHeader = getTableHeader();
        if (tableHeader != null) {
            tableHeader.setCursor(Cursors.handCursor());
        }
    }

    protected void adjustRowHeight(int padding) {
        rowVerticalPadding = padding;
        adjustRowHeight();
    }

    protected void adjustRowHeight() {
        Font font = getFont();
        FontRenderContext fontRenderContext = getFontMetrics(font).getFontRenderContext();
        LineMetrics lineMetrics = font.getLineMetrics("ABCÄÜÖÂÇĞIİÖŞĀČḎĒËĠḤŌŠṢṬŪŽy", fontRenderContext);
        int fontHeight = Math.round(lineMetrics.getHeight());
        setRowHeight(fontHeight + (rowVerticalPadding * 2));
    }

    @Override
    @NotNull
    public T getModel() {
        return Failsafe.nn((T) super.getModel());
    }

    private void calculateScrollDistance() {
        JViewport viewport = getViewport();
        if (viewport == null) return;

        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        if (pointerInfo == null) return;

        double mouseLocation = pointerInfo.getLocation().getX();
        double viewportLocation = viewport.getLocationOnScreen().getX();

        Point viewPosition = viewport.getViewPosition();
        double contentLocation = viewport.getView().getLocationOnScreen().getX();

        if (contentLocation < viewportLocation && mouseLocation < viewportLocation + 20) {
            scrollDistance = - Math.min(viewPosition.x, (viewportLocation - mouseLocation));
        } else {
            int viewportWidth = viewport.getWidth();
            int contentWidth = viewport.getView().getWidth();

            if (contentLocation + contentWidth > viewportLocation + viewportWidth && mouseLocation > viewportLocation + viewportWidth - 20) {
                scrollDistance = (mouseLocation - viewportLocation - viewportWidth);
            } else {
                scrollDistance = 0;
            }
        }
    }

    @NotNull
    public final Project getProject() {
        return parentComponent.ensure().ensureProject();
    }

    @NotNull
    public DBNComponent getParentComponent() {
        return parentComponent.ensure();
    }

    protected Object getValueAtMouseLocation() {
        Point location = MouseInfo.getPointerInfo().getLocation();
        location.setLocation(location.getX() - getLocationOnScreen().getX(), location.getY() - getLocationOnScreen().getY());
        return getValueAtLocation(location);
    }

    private Object getValueAtLocation(Point point) {
        int columnIndex = columnAtPoint(point);
        int rowIndex = rowAtPoint(point);
        return columnIndex > -1 && rowIndex > -1 ? getModel().getValueAt(rowIndex, columnIndex) : null;
    }

    /*********************************************************
     *                    Cell metrics                       *
     *********************************************************/
    public void accommodateColumnsSize() {
        int buffer = getColumnWidthBuffer();
        for (int c = 0; c < getColumnCount(); c++){
            accommodateColumnSize(c, buffer);
        }
    }

    public int getColumnWidthBuffer() {
        return 22;
    }

    @Override
    public int convertColumnIndexToView(int modelColumnIndex) {
        return super.convertColumnIndexToView(modelColumnIndex);
    }

    @Override
    public int convertColumnIndexToModel(int viewColumnIndex) {
        // table is not scrolling correctly when columns are moved
/*
        if (getTableHeader().getDraggedColumn() != null && CommonUtil.isCalledThrough(BasicTableHeaderUI.MouseInputHandler.class)) {
            return getTableHeader().getDraggedColumn().getModelIndex();
        }
*/
        return super.convertColumnIndexToModel(viewColumnIndex);
    }

    public void accommodateColumnSize(int columnIndex, int span) {
        TableColumnModel columnModel = getColumnModel();
        if (columnIndex < columnModel.getColumnCount()) {

            T model = getModel();
            TableColumn column = columnModel.getColumn(columnIndex);

            int minWidth = getMinColumnWidth();
            int maxWidth = getMaxColumnWidth();
            int preferredWidth = 0;

            // header
            JTableHeader tableHeader = getTableHeader();
            if (tableHeader != null) {
                Object headerValue = column.getHeaderValue();
                TableCellRenderer renderer = column.getHeaderRenderer();
                if (renderer == null) renderer = tableHeader.getDefaultRenderer();
                Component headerComponent = renderer.getTableCellRendererComponent(this, headerValue, false, false, 0, columnIndex);
                int width = (int) headerComponent.getPreferredSize().getWidth();
                if (width > preferredWidth)
                    preferredWidth = width;
            }

            // rows
            String columnName = model.getColumnName(columnIndex);
            int rowCount = model.getRowCount();
            for (int r = 0; r < rowCount; r++) {
                if (preferredWidth >= maxWidth) break;

                int c = column.getModelIndex();
                Object value = model.getValueAt(r, c);
                if (value == null) continue;

                String displayValue = model.getPresentableValue(value, c);
                if (displayValue == null || displayValue.length() >= 100) continue;

                int cellWidth = metricsCache.getTextWidth(columnName, displayValue);
                preferredWidth = Math.max(preferredWidth, cellWidth);
            }

            preferredWidth = Math.min(preferredWidth, maxWidth);
            preferredWidth = Math.max(preferredWidth, minWidth);
            preferredWidth = preferredWidth + span;

            if (column.getPreferredWidth() != preferredWidth)  {
                column.setPreferredWidth(preferredWidth);
            }
        }
    }

    protected int getMinColumnWidth() {
        return MIN_COLUMN_WIDTH;
    }

    protected int getMaxColumnWidth() {
        return MAX_COLUMN_WIDTH;
    }

    public void selectCell(int rowIndex, int columnIndex) {
        if (rowIndex > -1 && columnIndex > -1 && rowIndex < getRowCount() && columnIndex < getColumnCount()) {
            Rectangle cellRect = getCellRect(rowIndex, columnIndex, true);
            if (!getVisibleRect().contains(cellRect)) {
                scrollRectToVisible(cellRect);
            }
            if (getSelectedRowCount() != 1 || getSelectedRow() != rowIndex) {
                setRowSelectionInterval(rowIndex, rowIndex);
            }

            if (getSelectedColumnCount() != 1 || getSelectedColumn() != columnIndex) {
                setColumnSelectionInterval(columnIndex, columnIndex);
            }
        }

    }

    public String getPresentableValueAt(int selectedRow, int selectedColumn) {
        Object value = getValueAt(selectedRow, selectedColumn);
        String presentableValue;
        try {
            presentableValue = getModel().getPresentableValue(value, selectedColumn);
        } catch (UnsupportedOperationException e) {
            conditionallyLog(e);
            presentableValue = value == null ? null : value.toString();
        }
        return presentableValue;
    }

    private class ScrollTask extends TimerTask {
        @Override
        public void run() {
            JViewport viewport = getViewport();
            if (viewport == null || scrollDistance == 0) return;

            Dispatch.run(() -> {
                Point viewPosition = viewport.getViewPosition();
                viewport.setViewPosition(new Point((int) (viewPosition.x + scrollDistance), viewPosition.y));
                calculateScrollDistance();
            });
        }
    }

    protected DBNTableGutter<?> createTableGutter() {
        return null; // do not create gutter by default
    }

    public final DBNTableGutter<?> getTableGutter() {
        return tableGutter.get();
    }

    public final void initTableGutter() {
        DBNTableGutter tableGutter = getTableGutter();
        if (tableGutter == null) return;

        JScrollPane scrollPane = UIUtil.getParentOfType(JScrollPane.class, this);
        if (scrollPane == null) return;

        scrollPane.setRowHeaderView(tableGutter);
    }

    protected void resetTableGutter() {
        tableGutter.reset();
        initTableGutter();
    }

    public void stopCellEditing() {
        if (!isEditing()) return;

        TableCellEditor cellEditor = getCellEditor();
        if (cellEditor == null) return;

        cellEditor.stopCellEditing();
    }

    public Point getCellLocation(int row, int column) {
        Rectangle rectangle = getCellRect(row, column, true);
        Point location = getLocationOnScreen();
        return new Point(
                (int) (location.getX() + rectangle.getX()),
                (int) (location.getY() + rectangle.getY()));
    }

    public TableColumn getColumnByName(String columnName) {
        TableColumnModel columnModel = getColumnModel();
        int columnCount = columnModel.getColumnCount();
        for (int i=0; i < columnCount; i++) {
            TableColumn column = columnModel.getColumn(i);
            Object modelColumnIdentifier = column.getIdentifier();
            String modelColumnName = modelColumnIdentifier == null ? null : modelColumnIdentifier.toString();
            if (Strings.equalsIgnoreCase(columnName, modelColumnName)) {
                return column;
            }
        }
        return null;
    }

    @Override
    public void createDefaultColumnsFromModel() {
        TableModel tableModel = getModel();
        // Remove any current columns
        TableColumnModel columnModel = getColumnModel();
        Map<String, TableColumn> oldColumns = new HashMap<>();

        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            TableColumn column = columnModel.getColumn(i);
            Object headerValue = column.getHeaderValue();
            if (headerValue instanceof String) {
                oldColumns.put(headerValue.toString(), column);
            }
        }
        boolean columnSelectionAllowed = columnModel.getColumnSelectionAllowed();

        columnModel = new DefaultTableColumnModel();
        columnModel.setColumnSelectionAllowed(columnSelectionAllowed);

        // Create new columns from the data model info
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            String columnName = tableModel.getColumnName(i);
            TableColumn oldColumn = oldColumns.get(columnName);

            TableColumn newColumn = new TableColumn(i);
            newColumn.setHeaderValue(columnName);
            if (oldColumn != null) {
                newColumn.setPreferredWidth(oldColumn.getPreferredWidth());
            }
            columnModel.addColumn(newColumn);
        }
        setColumnModel(columnModel);
    }
    /********************************************************
     *                    Disposable                        *
     ********************************************************/

    @Nullable
    @Override
    public <V> V getUserData(@NotNull Key<V> key) {
        return userData.get(key);
    }

    @Override
    public <V> void putUserData(@NotNull Key<V> key, @Nullable V value) {
        userData = value == null ?
                userData.minus(key) :
                userData.plus(key, value);
    }

    protected void checkRowBounds(int rowIndex) {
        getModel().checkRowBounds(rowIndex);
    }

    protected void checkColumnBounds(int columnIndex) {
        getModel().checkColumnBounds(columnIndex);
    }

    /********************************************************
     *                    Disposable                        *
     ********************************************************/
    @Getter
    @Setter
    private boolean disposed;

    public void disposeInner(){
        Disposer.dispose(super.getModel());
        listenerList = new EventListenerList();
        columnModel = new DefaultTableColumnModel();
        selectionModel = new DefaultListSelectionModel();
        removeListeners(this);
        nullify();
    }
}
