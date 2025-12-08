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

package com.dbn.assistant.profile.ui;

import com.dbn.common.ui.table.DBNColoredTableCellRenderer;
import com.dbn.common.ui.table.DBNTable;
import org.jetbrains.annotations.Nullable;

public class AssistantProfilesTableCellRenderer extends DBNColoredTableCellRenderer {

    @Override
    protected void customizeCellRenderer(DBNTable table, @Nullable Object value, boolean selected, boolean hasFocus, int row, int column) {
        if (value == null) return;
        append(value.toString());
    }
}
