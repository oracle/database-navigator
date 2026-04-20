package com.dbn.mcp.ui;

import com.dbn.mcp.model.ParamRow;
import com.dbn.mcp.model.ParamType;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class ParamTableModel extends AbstractTableModel {
    private static final String[] COLUMNS_WITH_TEST = {"Name", "Type", "Test Value", "Description", "Required"};
    private static final String[] COLUMNS_SCHEMA = {"Name", "Type", "Description", "Required"};

    private final List<ParamRow> rows = new ArrayList<>();
    private final boolean includeTestValue;

    public ParamTableModel() {
        this(true);
    }

    public ParamTableModel(boolean includeTestValue) {
        this.includeTestValue = includeTestValue;
    }

    public List<ParamRow> getRows() { return rows; }

    @Override public int getRowCount() { return rows.size(); }
    @Override public int getColumnCount() { return includeTestValue ? COLUMNS_WITH_TEST.length : COLUMNS_SCHEMA.length; }
    @Override public String getColumnName(int col) { return includeTestValue ? COLUMNS_WITH_TEST[col] : COLUMNS_SCHEMA[col]; }

    @Override
    public Class<?> getColumnClass(int col) {
        switch (col) {
            case 1:
                return ParamType.class;
            case 4:
                return includeTestValue ? Boolean.class : String.class;
            case 3:
                return includeTestValue ? String.class : Boolean.class;
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
            case 2: return includeTestValue ? r.getTestValue() : r.getDescription();
            case 3: return includeTestValue ? r.getDescription() : r.isRequired();
            case 4: return includeTestValue ? r.isRequired() : null;
            default: return null;
        }
    }

    @Override
    public void setValueAt(Object val, int row, int col) {
        ParamRow r = rows.get(row);
        switch (col) {
            case 1: r.setType(val instanceof ParamType ? (ParamType) val : ParamType.STRING); break;
            case 2:
                if (includeTestValue) {
                    r.setTestValue(val != null ? val.toString() : "");
                } else {
                    r.setDescription(val != null ? val.toString() : "");
                }
                break;
            case 3:
                if (includeTestValue) {
                    r.setDescription(val != null ? val.toString() : "");
                } else {
                    r.setRequired(val instanceof Boolean && (Boolean) val);
                }
                break;
            case 4:
                if (includeTestValue) {
                    r.setRequired(val instanceof Boolean && (Boolean) val);
                }
                break;
        }
        fireTableRowsUpdated(row, row);
    }
}
