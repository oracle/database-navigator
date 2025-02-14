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

import com.dbn.common.color.Colors;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.event.ApplicationEvents;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.util.Borders;
import com.intellij.openapi.editor.colors.EditorColorsListener;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import java.awt.event.MouseEvent;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;

@Getter
@Setter
public abstract class DBNTableGutter<T extends DBNTableWithGutter> extends JList implements StatefulDisposable, EditorColorsListener {
    private boolean disposed;
    private final WeakRef<T> table;

    public DBNTableGutter(T table) {
        super(table.getModel().getListModel());
        this.table = WeakRef.of(table);
        int rowHeight = table.getRowHeight();
        if (rowHeight != 0) setFixedCellHeight(rowHeight);
        setBackground(Colors.getPanelBackground());
        setBorder(Borders.EMPTY_BORDER);
        setFocusable(false);
        setRequestFocusEnabled(false);

        setCellRenderer(createCellRenderer());

        ApplicationEvents.subscribe(this, EditorColorsManager.TOPIC, this);
        Disposer.register(table, this);
        setAccessibleName(this, "Table gutter");
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        return null;
    }

    protected abstract ListCellRenderer createCellRenderer();

    @Override
    public void globalSchemeChange(@Nullable EditorColorsScheme scheme) {
        setCellRenderer(createCellRenderer());
    }

    @Override
    public ListModel<?> getModel() {
        ListModel<?> current = super.getModel();

        if (this.table != null) {
            // only after initialization
            ListModel<?> delegate = getTable().getModel().getListModel();
            if (delegate != null && delegate != current) {
                setModel(delegate);
            }
        }
        return super.getModel();
    }

    @NotNull
    public T getTable() {
        return table.ensure();
    }

    @Override
    public void disposeInner() {
        nullify();
    }
}
