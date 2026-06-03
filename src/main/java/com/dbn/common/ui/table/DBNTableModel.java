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

import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.exception.OutdatedContentException;
import com.dbn.nls.NlsSupport;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.table.TableModel;

public interface DBNTableModel<R> extends TableModel, StatefulDisposable, NlsSupport {
    @Override
    @Nls
    String getColumnName(int columnIndex);

    default Object getValue(R rowObject, int column) {
        throw new UnsupportedOperationException();
    };

    default String getPresentableValue(R rowObject, int column) {
        return rowObject == null ? "" : rowObject.toString();
    }

    default SimpleTextAttributes getAttributes(R rowObject, int column) {
        return SimpleTextAttributes.REGULAR_ATTRIBUTES;
    }

    @Nullable
    default Icon getIcon(R rowObject, int column) { return null; }

    @Nullable
    default String getTooltip(R rowObject, int column) {
        return null;
    }

    default void checkRowBounds(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= getRowCount()) throw new OutdatedContentException(this);
    }

    default void checkColumnBounds(int columnIndex) {
        if (columnIndex < 0 || columnIndex >= getColumnCount()) throw new OutdatedContentException(this);
    }

    default boolean isLargeValue(int columnIndex) {
        return false;
    }

    default boolean isPresentableLargeValue(int columnIndex) {
        return false;
    }

    default boolean isEmpty() {
        return getRowCount() == 0;
    }
}
