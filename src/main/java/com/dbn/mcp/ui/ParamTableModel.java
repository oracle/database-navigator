package com.dbn.mcp.ui;

import com.dbn.mcp.model.ParamRow;
import com.dbn.mcp.model.ParamType;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class ParamTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {"Name", "Type", "Description", "Required"};
    private final List<ParamRow> rows = new ArrayList<>();

    public List<ParamRow> getRows() { return rows; }

    @Override public int getRowCount() { return rows.size(); }
    @Override public int getColumnCount() { return COLUMNS.length; }
    @Override public String getColumnName(int col) { return COLUMNS[col]; }

    @Override
    public Class<?> getColumnClass(int col) {
        switch (col) {
            case 1: return ParamType.class;
            case 3: return Boolean.class;
            default: return String.class;
        }
    }

    @Override public boolean isCellEditable(int row, int col) { return col > 0; }

    @Override
    public Object getValueAt(int row, int col) {
        ParamRow r = rows.get(row);
        switch (col) {
            case 0: return r.getName();
            case 1: return r.getType();
            case 2: return r.getDescription();
            case 3: return r.isRequired();
            default: return null;
        }
    }

    @Override
    public void setValueAt(Object val, int row, int col) {
        ParamRow r = rows.get(row);
        switch (col) {
            case 1: r.setType(val instanceof ParamType ? (ParamType) val : ParamType.STRING); break;
            case 2: r.setDescription(val != null ? val.toString() : ""); break;
            case 3: r.setRequired(val instanceof Boolean && (Boolean) val); break;
        }
        fireTableRowsUpdated(row, row);
    }
}
