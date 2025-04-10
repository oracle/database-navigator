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

package com.dbn.data.grid.addon;

import com.dbn.common.addon.ComponentAddonBase;
import com.dbn.common.ui.util.Mouse;
import com.dbn.common.util.Dialogs;
import com.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dbn.data.grid.ui.table.resultSet.record.ResultSetRecordViewerDialog;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.ui.util.ClientProperty.DATA_TYPE_RELEVANT;
import static com.dbn.common.ui.util.ClientProperty.RECORD_VIEWER_ADDON;
import static java.awt.event.MouseEvent.BUTTON1;

public class RecordViewerAddon extends ComponentAddonBase<ResultSetTable> {

    private RecordViewerAddon(ResultSetTable component) {
        super(component);
        Mouse.onMouseClick(getComponent(), BUTTON1, 2, e -> showRecordDetails());
    }

    public void showRecordDetails() {
        Dialogs.show(() -> {
            ResultSetTable table = getComponent();
            boolean showDataTypes = DATA_TYPE_RELEVANT.get(table);
            return new ResultSetRecordViewerDialog(table, showDataTypes);
        });
    }

    public static void installTo(ResultSetTable table) {
        RECORD_VIEWER_ADDON.get(table, () -> new RecordViewerAddon(table));
    }

    @Nullable
    public static RecordViewerAddon of(ResultSetTable table) {
        return RECORD_VIEWER_ADDON.get(table);
    }

    public static void uninstallFrom(ResultSetTable table) {
        RECORD_VIEWER_ADDON.set(table, null);
    }
}
