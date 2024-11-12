package com.dbn.data.grid.ui.table.basic;

import com.dbn.common.color.Colors;
import com.dbn.common.event.ApplicationEvents;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.locale.options.RegionalSettings;
import com.dbn.common.locale.options.RegionalSettingsListener;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.table.DBNTableGutter;
import com.dbn.common.ui.table.DBNTableHeaderRenderer;
import com.dbn.common.ui.table.DBNTableWithGutter;
import com.dbn.common.ui.table.TableSelectionRestorer;
import com.dbn.common.ui.util.Fonts;
import com.dbn.common.ui.util.Mouse;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.MathResult;
import com.dbn.common.util.Safe;
import com.dbn.data.grid.color.DataGridTextAttributes;
import com.dbn.data.grid.options.DataGridSettings;
import com.dbn.data.model.ColumnInfo;
import com.dbn.data.model.DataModelCell;
import com.dbn.data.model.DataModelRow;
import com.dbn.data.model.DataModelState;
import com.dbn.data.model.basic.BasicDataModel;
import com.dbn.data.model.basic.BasicDataModelCell;
import com.dbn.data.preview.LargeValuePreviewPopup;
import com.dbn.data.value.LargeObjectValue;
import com.intellij.ide.IdeTooltip;
import com.intellij.ide.IdeTooltipManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.colors.EditorColorsListener;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.components.JBViewport;
import com.intellij.util.Alarm;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.dbn.common.ui.util.Mouse.isMainSingleClick;
import static com.dbn.common.util.Conditional.when;

@Getter
public class BasicTable<T extends BasicDataModel<?, ?>> extends DBNTableWithGutter<T> implements EditorColorsListener, Disposable {
    private final BasicTableCellRenderer cellRenderer;
    private final RegionalSettings regionalSettings;
    private final DataGridSettings dataGridSettings;
    private final TableSelectionRestorer selectionRestorer = createSelectionRestorer();
    private JBPopup valuePopup;
    private MathResult selectionMath;
    private boolean loading;

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
        addMouseListener(Mouse.listener().onClick(
                e -> when(isMainSingleClick(e) && valuePopup == null,
                () -> showCellValuePopup())));

        addPropertyChangeListener(e -> {
            Object newProperty = e.getNewValue();
            if (newProperty instanceof Font) {
                Font font = (Font) newProperty;
                adjustRowHeight();
                JTableHeader tableHeader = getTableHeader();
                if (tableHeader != null) {
                    TableCellRenderer defaultRenderer = tableHeader.getDefaultRenderer();
                    if (defaultRenderer instanceof DBNTableHeaderRenderer) {
                        DBNTableHeaderRenderer renderer = (DBNTableHeaderRenderer) defaultRenderer;
                        renderer.setFont(font);
                    }
                }
                accommodateColumnsSize();
            }

        });

        getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            selectionMath = null;

            BigDecimal total = BigDecimal.ZERO;
            BigDecimal count = BigDecimal.ZERO;
            int rows = getSelectedRowCount();
            int columns = getSelectedColumnCount();
            if (columns != 1 || rows <= 1 || rows >= 200) return;

            int selectedColumn = getSelectedColumn();
            int[] selectedRows = getSelectedRows();
            for (int selectedRow : selectedRows) {
                Object value = getValueAt(selectedRow, selectedColumn);
                if (value instanceof BasicDataModelCell) {
                    BasicDataModelCell<?, ?> cell = (BasicDataModelCell<?, ?>) value;
                    Object userValue = cell.getUserValue();
                    if (userValue == null || userValue instanceof Number) {
                        if (userValue != null) {
                            count = count.add(BigDecimal.ONE);
                            Number number = (Number) userValue;
                            total = total.add(new BigDecimal(number.toString()));
                        }

                    } else {
                        return;
                    }
                } else {
                    return;
                }
            }
            if (count.compareTo(BigDecimal.ZERO) <= 0) return;

            BigDecimal average = total.divide(count, 9, RoundingMode.HALF_UP);
            average = average.stripTrailingZeros();
            selectionMath = new MathResult(total, count, average);
            showSelectionTooltip();
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            private final Alarm runner = Dispatch.alarm(BasicTable.this);
            @Override
            public void mouseMoved(MouseEvent e) {
                if (selectionMath != null && isCellSelected(e.getPoint())) {
                    Dispatch.alarmRequest(runner, 100, true, () -> showSelectionTooltip());
                }
            }
        });

        ProjectEvents.subscribe(project, this, RegionalSettingsListener.TOPIC, regionalSettingsListener);
        ApplicationEvents.subscribe(this, EditorColorsManager.TOPIC, this);

        //EventUtil.subscribe(this, UISettingsListener.TOPIC, this);
    }

    boolean isCellSelected(Point point) {
        DataModelCell<?, ?> cell = getCellAtLocation(point);
        if (cell == null) return false;

        int rowIndex = cell.getRow().getIndex();
        int columnIndex = cell.getColumnInfo().getIndex();
        return isCellSelected(rowIndex, columnIndex);
    }

    private void showSelectionTooltip() {
        MathResult mathResult = this.selectionMath;
        if (mathResult == null) return;

        Point mousePosition = getMousePosition();
        if (mousePosition == null) return;
        if (!isCellSelected(mousePosition)) return;

        MathPanel mathPanel = new MathPanel(getProject(), mathResult);
        IdeTooltip tooltip = new IdeTooltip(this, mousePosition, mathPanel.getComponent());
        tooltip.setFont(Fonts.deriveFont(Fonts.REGULAR,  (float) 16));
        IdeTooltipManager.getInstance().show(tooltip, true);
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

    public void setLoading(boolean loading) {
        this.loading = loading;
        updateBackground(loading);
    }

    public void updateBackground(boolean readonly) {
        Dispatch.run(() -> {
            JBViewport viewport = UIUtil.getParentOfType(JBViewport.class, this);
            DataGridTextAttributes attributes = cellRenderer.getAttributes();
            Color background = readonly ?
                    attributes.getLoadingData(false).getBgColor() :
                    attributes.getPlainData(false, false).getBgColor();

            if (viewport != null) {
                viewport.setBackground(background);
                viewport.getParent().setBackground(background);
                UserInterface.repaint(viewport);
            }
        });

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
            accommodateColumnsSize();
            resetTableGutter();
        }

        DBNTableGutter<?> tableGutter = getTableGutter();
        if (tableGutter == null) return;

        tableGutter.setFixedCellHeight(rowHeight);
        tableGutter.setFixedCellWidth(getModel().getRowCount() == 0 ? 10 : -1);
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
        updateBackground(loading);
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
        if (!e.getValueIsAdjusting()) {
            if (hasFocus()) getTableGutter().clearSelection();
            showCellValuePopup();
        }
    }

    @Override
    public void columnSelectionChanged(ListSelectionEvent e) {
        JTableHeader tableHeader = getTableHeader();
        if (tableHeader != null && tableHeader.getDraggedColumn() == null) {
            super.columnSelectionChanged(e);
            if (!e.getValueIsAdjusting()) {
                showCellValuePopup();
            }
        }
    }

    private void showCellValuePopup() {
        if (valuePopup != null) {
            valuePopup.cancel();
            valuePopup = null;
        }
        if (!isLargeValuePopupActive()) return;
        if (isRestoringSelection()) return;
        if (!isShowing()) return;
        if (getSelectedRowCount() != 1) return;
        if (getSelectedColumnCount() != 1) return;

        T model = getModel();
        DataModelState modelState = model.getState();
        boolean isReadonly = model.isReadonly() || model.isEnvironmentReadonly() || modelState.isReadonly() ;
        if (!isReadonly) return;

        int rowIndex = getSelectedRow();
        int columnIndex = getSelectedColumn();
        if (canDisplayCompleteValue(rowIndex, columnIndex)) return;

        Rectangle cellRect = getCellRect(rowIndex, columnIndex, true);
        DataModelCell<?, ?> cell = (DataModelCell<?, ?>) getValueAt(rowIndex, columnIndex);
        TableColumn column = getColumnModel().getColumn(columnIndex);

        int preferredWidth = column.getWidth();
        LargeValuePreviewPopup viewer = new LargeValuePreviewPopup(getProject(), this, cell, preferredWidth);
        initLargeValuePopup(viewer);
        Point location = cellRect.getLocation();
        location.setLocation(location.getX() + 4, location.getY() + 20);

        valuePopup = viewer.show(this, location);
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

    protected void initLargeValuePopup(LargeValuePreviewPopup viewer) {
    }

    protected boolean isLargeValuePopupActive() {
        return true;
    }

    @Nullable
    public MathResult getSelectionMath() {
        return selectionMath;
    }

    private boolean canDisplayCompleteValue(int rowIndex, int columnIndex) {
        DataModelCell<?, ?> cell = (DataModelCell<?, ?>) getValueAt(rowIndex, columnIndex);
        if (cell != null) {
            Object value = cell.getUserValue();
            if (value instanceof LargeObjectValue) {
                return false;
            }
            if (value != null) {
                TableCellRenderer renderer = getCellRenderer(rowIndex, columnIndex);
                Component component = renderer.getTableCellRendererComponent(this, cell, false, false, rowIndex, columnIndex);
                TableColumn column = getColumnModel().getColumn(columnIndex);
                return component.getPreferredSize().width <= column.getWidth();
            }
        }
        return true;
    }

    public Rectangle getCellRect(DataModelCell<?, ?> cell) {
        int rowIndex = convertRowIndexToView(cell.getRow().getIndex());
        int columnIndex = convertColumnIndexToView(cell.getIndex());
        return getCellRect(rowIndex, columnIndex, true);
    }

    public void scrollCellToVisible(DataModelCell<?, ?> cell) {
        Rectangle cellRectangle = getCellRect(cell);
        scrollRectToVisible(cellRectangle);
    }

    @NotNull
    @Override
    public T getModel() {
        return super.getModel();
    }

}
