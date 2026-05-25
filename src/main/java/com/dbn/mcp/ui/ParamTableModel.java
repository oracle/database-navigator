package com.dbn.mcp.ui;

import com.dbn.mcp.model.McpToolDefinition;
import com.dbn.mcp.model.McpToolParam;
import com.dbn.mcp.model.McpToolParamType;
import lombok.Getter;

import javax.swing.table.AbstractTableModel;
import java.util.List;

@Getter
public class ParamTableModel extends AbstractTableModel {
    private static final String[] COLUMNS_WITH_TEST = {"Name", "Type", "Test Value", "Description", "Required"};
    private static final String[] COLUMNS_SCHEMA = {"Name", "Type", "Description", "Required"};

    private final boolean includeTestValue;
    private final McpToolDefinition toolDefinition;

    public ParamTableModel(McpToolDefinition toolDefinition) {
        this(toolDefinition, true);
    }

    public ParamTableModel(McpToolDefinition toolDefinition, boolean includeTestValue) {
        this.includeTestValue = includeTestValue;
        this.toolDefinition = toolDefinition;
    }

    @Override public int getRowCount() { return getRows().size(); }

    public List<McpToolParam> getRows() {
        return toolDefinition.getParameters();
    }

    @Override public int getColumnCount() { return includeTestValue ? COLUMNS_WITH_TEST.length : COLUMNS_SCHEMA.length; }
    @Override public String getColumnName(int col) { return includeTestValue ? COLUMNS_WITH_TEST[col] : COLUMNS_SCHEMA[col]; }

    @Override
    public Class<?> getColumnClass(int col) {
        return switch (col) {
            case 1 -> McpToolParamType.class;
            case 4 -> includeTestValue ? Boolean.class : String.class;
            case 3 -> includeTestValue ? String.class : Boolean.class;
            default -> String.class;
        };
    }

    @Override public boolean isCellEditable(int row, int col) { return col > 0; }

    @Override
    public Object getValueAt(int row, int col) {
        McpToolParam r = getRows().get(row);
        return switch (col) {
            case 0 -> r.getName();
            case 1 -> r.getType();
            case 2 -> includeTestValue ? r.getTestValue() : r.getDescription();
            case 3 -> includeTestValue ? r.getDescription() : r.isRequired();
            case 4 -> includeTestValue ? r.isRequired() : null;
            default -> null;
        };
    }

    @Override
    public void setValueAt(Object val, int row, int col) {
        McpToolParam r = getRows().get(row);
        switch (col) {
            case 1: r.setType(val instanceof McpToolParamType ? (McpToolParamType) val : McpToolParamType.STRING); break;
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
