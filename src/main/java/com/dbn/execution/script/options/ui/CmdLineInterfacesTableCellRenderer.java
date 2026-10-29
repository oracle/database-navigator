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

package com.dbn.execution.script.options.ui;

import com.dbn.common.checksum.file.FileFingerprintStatus;
import com.dbn.common.ui.table.DBNColoredTableCellRenderer;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.util.Strings;
import com.dbn.connection.DatabaseType;
import com.dbn.common.ui.util.Tooltips;
import com.intellij.ui.AnimatedIcon;

import static com.dbn.nls.NlsResources.txt;
import static com.intellij.ui.SimpleTextAttributes.ERROR_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES;

public class CmdLineInterfacesTableCellRenderer extends DBNColoredTableCellRenderer {
    @Override
    protected void customizeCellRenderer(DBNTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
        setIcon(null);
        Tooltips.setToolTipText(this, null);
        if (value instanceof DatabaseType databaseType) {
            setIcon(databaseType.getIcon());
            append(databaseType.getName());
        }
        else if (value instanceof FileFingerprintStatus status) {
            renderFingerprintStatus(status);
        }
        else if (value instanceof String stringValue) {
            if (Strings.isNotEmpty(stringValue)) {
                append(stringValue, REGULAR_ATTRIBUTES);
            }
        }
    }

    private void renderFingerprintStatus(FileFingerprintStatus status) {
        switch (status) {
            case CALCULATING -> {
                setIcon(AnimatedIcon.Default.INSTANCE);
                append(txt("cfg.execution.const.FileFingerprintStatus_CALCULATING"), GRAY_ITALIC_ATTRIBUTES);
                Tooltips.setToolTipText(this, txt("cfg.execution.tooltip.ExecutableFingerprintCalculating"));
            }
            case READY -> {
                append(txt("cfg.execution.const.FileFingerprintStatus_READY"), REGULAR_ATTRIBUTES);
                Tooltips.setToolTipText(this, txt("cfg.execution.tooltip.ExecutableFingerprintReady"));
            }
            case FAILED -> {
                append(txt("cfg.execution.const.FileFingerprintStatus_FAILED"), ERROR_ATTRIBUTES);
                Tooltips.setToolTipText(this, txt("cfg.execution.tooltip.ExecutableFingerprintFailed"));
            }
            case EMPTY -> {
            }
        }
    }
}
