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

import com.dbn.common.util.Commons;
import com.intellij.openapi.ide.CopyPasteManager;
import org.jdesktop.swingx.plaf.basic.core.BasicTransferable;
import org.jetbrains.annotations.NonNls;

import javax.swing.JComponent;
import javax.swing.TransferHandler;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;

public class DBNTableTransferHandler extends TransferHandler {
    public static final DBNTableTransferHandler INSTANCE = new DBNTableTransferHandler();

    private DBNTableTransferHandler() {}

    @Override
    public void exportToClipboard(JComponent comp, Clipboard clip, int action) {
        DBNTable table = (DBNTable) comp;
        Transferable content = createClipboardContent(table);
        if (content != null) {
            //clip.setContents(contents, null);

            CopyPasteManager copyPasteManager = CopyPasteManager.getInstance();
            copyPasteManager.setContents(content);
        }
    }

    protected Transferable createClipboardContent(DBNTable table) {
        int[] rows;
        int[] cols;

        if (!table.getRowSelectionAllowed() && !table.getColumnSelectionAllowed()) {
            return null;
        }

        if (!table.getRowSelectionAllowed()) {
            int rowCount = table.getRowCount();

            rows = new int[rowCount];
            for (int counter = 0; counter < rowCount; counter++) {
                rows[counter] = counter;
            }
        } else {
            rows = table.getSelectedRows();
        }

        if (!table.getColumnSelectionAllowed()) {
            int colCount = table.getColumnCount();

            cols = new int[colCount];
            for (int counter = 0; counter < colCount; counter++) {
                cols[counter] = counter;
            }
        } else {
            cols = table.getSelectedColumns();
        }

        if (rows == null || cols == null || rows.length == 0 || cols.length == 0) {
            return null;
        }

        StringBuilder plainStr = new StringBuilder();
        @NonNls StringBuilder htmlStr = new StringBuilder();

        htmlStr.append("<html>\n<body>\n<table>\n");

        for (int row = 0; row < rows.length; row++) {
            htmlStr.append("<tr>\n");
            for (int col = 0; col < cols.length; col++) {
                String presentable = table.getPresentableValueAt(rows[row], cols[col]);
                String val = Commons.nvl(presentable, "");
                plainStr.append(val).append('\t');
                htmlStr.append("  <td>").append(val).append("</td>\n");
            }
            // we want a newline at the end of each line and not a tab
            plainStr.deleteCharAt(plainStr.length() - 1).append('\n');
            htmlStr.append("</tr>\n");
        }

        // remove the last newline
        plainStr.deleteCharAt(plainStr.length() - 1);
        htmlStr.append("</table>\n</body>\n</html>");

        return new BasicTransferable(plainStr.toString(), htmlStr.toString());
    }

}
