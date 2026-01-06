package com.dbn.vector.ui.source.ui;

import com.dbn.common.icon.Icons;
import com.dbn.common.ui.list.ColoredListCellRenderer;
import com.dbn.vector.model.sourceconfig.DbTableSource;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class DBTableListCellRenderer extends ColoredListCellRenderer<DbTableSource> {
    @Override
    protected void customize(@NotNull JList<? extends DbTableSource> list, DbTableSource value, int index, boolean selected, boolean hasFocus) {
        if (value == null) {
            append("(null)", SimpleTextAttributes.ERROR_ATTRIBUTES);
            return;
        }

        String schemaName = value.getSchemaName();
        String tableName = value.getTableName();
        String keyColumn = value.getKeyColumnName();
        String dataColumn = value.getDataColumnName();

        // Schema.Table
        if (schemaName != null && !schemaName.isEmpty()) {
            append(schemaName + ".", SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }

        if (tableName != null && !tableName.isEmpty()) {
            append(tableName, list.isEnabled() ?
                    SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES :
                    SimpleTextAttributes.GRAY_ATTRIBUTES);
        } else {
            append("(no table)", SimpleTextAttributes.ERROR_ATTRIBUTES);
        }

        // Column info
        if (keyColumn != null && dataColumn != null) {
            append("  ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
            append("ID: " + keyColumn + ", Data: " + dataColumn, SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }

        setIcon(Icons.DBO_TABLE);

        // Tooltip with full info
        StringBuilder tooltip = new StringBuilder();
        tooltip.append(schemaName != null ? schemaName : "?");
        tooltip.append(".");
        tooltip.append(tableName != null ? tableName : "?");
        tooltip.append("\nID Column: ").append(keyColumn != null ? keyColumn : "not set");
        tooltip.append("\nData Column: ").append(dataColumn != null ? dataColumn : "not set");
        setToolTipText(tooltip.toString());
    }
}
