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

package com.dbn.common.ui.table;

import com.dbn.common.data.Data;
import com.intellij.ui.SimpleTextAttributes;
import lombok.Getter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.ListModel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.dbn.common.util.Unsafe.cast;

@Getter
public class DBNDynamicTableModel<T> extends DBNMutableTableModel<T> implements DBNTableWithGutterModel<T> {
    private final ListModel gutterModel = new DBNTableGutterModel<>(this);

    private final Class<T> type;
    private final List<T> data;
    private final List<ColumnSpec<?>> columns = new ArrayList<>();

    public DBNDynamicTableModel(Class<T> type, List<T> data) {
        this.type = type;
        this.data = new ArrayList<>(data);
    }

    protected <V> ColumnSpec<V> addColumn(String name, Function<T, V> value) {
        ColumnSpec<V> columnSpec = new ColumnSpec<>(name, value);
        columns.add(columnSpec);
        return columnSpec;
    }

    @Override
    public final int getRowCount() {
        return data.size();
    }

    @Override
    public final int getColumnCount() {
        return columns.size();
    }

    @Override
    public final @Nls String getColumnName(int columnIndex) {
        return getColumn(columnIndex).getName();
    }

    @Override
    public final Class<?> getColumnClass(int columnIndex) {
        return type;
    }

    @Override
    public final T getValueAt(int rowIndex, int columnIndex) {
        return data.get(rowIndex);
    }

    @Override
    public final Object getValue(T row, int columnIndex) {
        Function<T, ?> value = getColumn(columnIndex).getValue();
        return value.apply(row);
    }

    @Override
    public Icon getIcon(T row, int columnIndex) {
        Function<T, Icon> icon = getColumn(columnIndex).getIcon();
        return icon == null ? null : icon.apply(row);
    }

    @Override
    public @Nullable String getTooltip(T rowObject, int column) {
        Function<T, String> tooltip = getColumn(column).getTooltip();
        return tooltip == null ? null : tooltip.apply(rowObject);
    }

    @Override
    public String getPresentableValue(T row, int column) {
        return Data.asString(getValue(row, column));
    }

    @Override
    public SimpleTextAttributes getAttributes(T rowObject, int column) {
        Function<T, SimpleTextAttributes> textAttributes = getColumn(column).getAttributes();
        return textAttributes == null ? SimpleTextAttributes.REGULAR_ATTRIBUTES : textAttributes.apply(rowObject);
    }

    private <V> ColumnSpec<V> getColumn(int columnIndex) {
        return cast(columns.get(columnIndex));
    }

    public T getData(int rowIndex) {
        return data.get(rowIndex);
    }

    @Getter
    public class ColumnSpec<V> {
        private final String name;
        private final Function<T, V> value;
        private Function<T, Icon> icon;
        private Function<T, String> tooltip;
        private Function<T, SimpleTextAttributes> attributes;
        public ColumnSpec(String name, Function<T, V> value) {
            this.name = name;
            this.value = value;
        }

        public ColumnSpec<V> withTooltip(Function<T, String> tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public ColumnSpec<V> withIcon(Function<T, Icon> icon) {
            this.icon = icon;
            return this;
        }

        public ColumnSpec<V> withAttributes(Function<T, SimpleTextAttributes> attributes) {
            this.attributes = attributes;
            return this;
        }
    }

    @Override
    public void disposeInner() {
        data.clear();
    }
}
