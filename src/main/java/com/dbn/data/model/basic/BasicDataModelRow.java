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

package com.dbn.data.model.basic;

import com.dbn.common.collections.CompactArrayList;
import com.dbn.common.dispose.Disposed;
import com.dbn.common.dispose.Disposer;
import com.dbn.data.model.DataModelCell;
import com.dbn.data.model.DataModelRow;
import com.dbn.editor.data.model.RecordStatus;
import com.dbn.editor.data.model.RecordStatusHolder;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.dbn.common.dispose.Failsafe.nd;

@Getter
@Setter
public class BasicDataModelRow<
        M extends BasicDataModel<? extends BasicDataModelRow<M, C>, C>,
        C extends DataModelCell<? extends BasicDataModelRow<M, C>, M>>
        extends RecordStatusHolder
        implements DataModelRow<M, C> {

    private M model;
    private List<C> cells;
    private int index;

    public BasicDataModelRow(M model) {
        cells = new CompactArrayList<>(model.getColumnCount());
        this.model = model;
    }

    @Override
    protected RecordStatus[] properties() {
        return RecordStatus.VALUES;
    }

    @Override
    @NotNull
    public M getModel() {
        return nd(model);
    }

    @Override
    public final C getCell(String columnName) {
        int columnIndex = getModel().getHeader().getColumnIndex(columnName);
        return columnIndex == -1 || columnIndex >= cells.size() ? null : cells.get(columnIndex);
    }

    @Override
    public final Object getCellValue(String columnName) {
        C cell = getCell(columnName);
        if (cell != null) {
            return cell.getUserValue();
        }
        return null;
    }

    @Nullable
    @Override
    public C getCellAtIndex(int index) {
        return index > -1 && cells.size() > index ? cells.get(index) : null;
    }

    public Project getProject() {
        return getModel().getProject();
    }

    @Override
    public void disposeInner() {
        cells = Disposer.replace(cells, Disposed.list());
        nullify();
    }
}
