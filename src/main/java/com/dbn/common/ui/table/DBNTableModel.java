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

import javax.swing.table.TableModel;

public interface DBNTableModel<R> extends TableModel, StatefulDisposable, NlsSupport {
    default String getPresentableValue(R rowObject, int column) {
        return rowObject == null ? "" : rowObject.toString();
    };

    default Object getValue(R rowObject, int column) {
        throw new UnsupportedOperationException();
    };

    default void checkRowBounds(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= getRowCount()) throw new OutdatedContentException(this);
    }

    default void checkColumnBounds(int columnIndex) {
        if (columnIndex < 0 || columnIndex >= getColumnCount()) throw new OutdatedContentException(this);
    }
}
