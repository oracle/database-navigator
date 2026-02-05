package com.dbn.mcp.ui;

import com.dbn.mcp.McpServerInputForm.ParamRow;
import com.dbn.mcp.McpServerInputForm.ParamType;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class ParamTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {"Name", "Type", "Default"};
    private final List<ParamRow> rows = new ArrayList<>();

    public List<ParamRow> getRows() { return rows; }

    @Override public int getRowCount() { return rows.size(); }
    @Override public int getColumnCount() { return COLUMNS.length; }
    @Override public String getColumnName(int col) { return COLUMNS[col]; }
    @Override public Class<?> getColumnClass(int col) { return col == 1 ? ParamType.class : String.class; }
    @Override public boolean isCellEditable(int row, int col) { return col > 0; }

    @Override
    public Object getValueAt(int row, int col) {
        ParamRow r = rows.get(row);
        switch (col) {
            case 0: return r.name;
            case 1: return r.type;
            case 2: return r.defaultValue;
            default: return null;
        }
    }

    @Override
    public void setValueAt(Object val, int row, int col) {
        ParamRow r = rows.get(row);
        if (col == 1) r.type = val instanceof ParamType ? (ParamType) val : ParamType.String;
        if (col == 2) r.defaultValue = val != null ? val.toString() : "";
        fireTableRowsUpdated(row, row);
    }
}
