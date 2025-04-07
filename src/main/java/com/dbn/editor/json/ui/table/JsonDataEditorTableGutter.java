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

package com.dbn.editor.json.ui.table;

import com.dbn.common.ui.util.Mouse;
import com.dbn.common.util.Conditional;
import com.dbn.data.grid.ui.table.basic.BasicTableGutter;
import com.dbn.editor.json.model.JsonDataEditorModel;

import javax.swing.ListCellRenderer;
import java.awt.event.MouseListener;

import static com.dbn.editor.data.model.RecordStatus.INSERTING;
import static com.dbn.editor.data.model.RecordStatus.MODIFIED;

public class JsonDataEditorTableGutter extends BasicTableGutter<JsonDataEditorTable> {
    public JsonDataEditorTableGutter(JsonDataEditorTable table) {
        super(table);
        addMouseListener(mouseListener);
    }

    @Override
    protected ListCellRenderer<?> createCellRenderer() {
        return new JsonDataEditorTableGutterRenderer();
    }

    @Override
    protected int getAdditionalSpacing() {
        JsonDataEditorModel model = getTableModel();
        return model.isOneOf(MODIFIED, INSERTING) ? 16 : 0;
    }

    private JsonDataEditorModel getTableModel() {
        return getTable().getModel();
    }

    MouseListener mouseListener = Mouse.listener().onClick(e ->
            Conditional.when(
                    Mouse.isMainDoubleClick(e),
                    () -> {})); // TODO any action on double click?

    @Override
    public void disposeInner() {
        removeMouseListener(mouseListener);
        mouseListener = null;
        super.disposeInner();
    }
}
