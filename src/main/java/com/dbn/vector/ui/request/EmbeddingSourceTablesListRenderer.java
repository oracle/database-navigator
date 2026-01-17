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

package com.dbn.vector.ui.request;

import com.dbn.common.icon.Icons;
import com.dbn.common.ui.list.ColoredListCellRenderer;
import com.dbn.vector.model.request.EmbeddingTableSource;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.JList;

import static com.dbn.common.util.Strings.isNotEmpty;
import static com.intellij.ui.SimpleTextAttributes.ERROR_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES;

public class EmbeddingSourceTablesListRenderer extends ColoredListCellRenderer<EmbeddingTableSource> {
    @Override
    protected void customize(@NotNull JList<? extends EmbeddingTableSource> list, EmbeddingTableSource value, int index, boolean selected, boolean hasFocus) {
        if (value == null) {
            append("(null)", ERROR_ATTRIBUTES);
            return;
        }

        String schemaName = value.getSchemaName();
        String tableName = value.getTableName();
        String keyColumn = value.getKeyColumnName();
        String dataColumn = value.getDataColumnName();

        // Schema.Table
        boolean listEnabled = list.isEnabled();
        SimpleTextAttributes regularAttributes = listEnabled ? REGULAR_ATTRIBUTES : GRAYED_ATTRIBUTES;
        if (isNotEmpty(schemaName)) {
            append(schemaName + ".", regularAttributes);
        }

        if (isNotEmpty(tableName)) {
            append(tableName, regularAttributes);
        } else {
            append("(no table)", ERROR_ATTRIBUTES);
        }

        // Column info
        if (isNotEmpty(keyColumn) && isNotEmpty(dataColumn)) {
            append("  ", GRAYED_ATTRIBUTES);
            append(keyColumn, GRAYED_ATTRIBUTES);
            append(" / ", GRAYED_ATTRIBUTES);
            append(dataColumn, GRAYED_ATTRIBUTES);
            //append("ID: " + keyColumn + ", Data: " + dataColumn, SimpleTextAttributes.GRAYED_ATTRIBUTES);
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
