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

package com.dbn.data.editor.ui.array;

import com.dbn.common.ui.list.EditableStringList;
import org.jetbrains.annotations.NotNull;

import javax.swing.table.TableCellEditor;
import java.awt.Component;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;

public class ArrayEditorList extends EditableStringList {
    public ArrayEditorList(ArrayEditorPopupProviderForm parent) {
        super(parent, false, true);
        setAccessibleName(this, "Array Editor");
    }

    @Override
    public @NotNull ArrayEditorPopupProviderForm getParentComponent() {
        return (ArrayEditorPopupProviderForm) super.getParentComponent();
    }

    @Override
    public Component prepareEditor(TableCellEditor editor, int rowIndex, int columnIndex) {
        Component component = super.prepareEditor(editor, rowIndex, columnIndex);
        component.addKeyListener(getParentComponent());
        return component;
    }
}
