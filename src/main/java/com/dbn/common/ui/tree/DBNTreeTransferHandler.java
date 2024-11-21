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

package com.dbn.common.ui.tree;

import com.dbn.common.util.Strings;
import com.intellij.openapi.ide.CopyPasteManager;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public class DBNTreeTransferHandler extends TransferHandler {
    public static DBNTreeTransferHandler INSTANCE = new DBNTreeTransferHandler();

    private DBNTreeTransferHandler() {}

    @Override
    public void exportToClipboard(JComponent comp, Clipboard clip, int action) {
        JTree tree = (JTree)comp;
        TreePath[] paths = tree.getSelectionPaths();
        if(paths != null && paths.length > 0) {
            StringBuilder builder = new StringBuilder();
            for (TreePath path : paths) {
                builder.append(path.getLastPathComponent().toString());
                builder.append("\n");
            }
            builder.delete(builder.length() - 1, builder.length());

            String contentString = builder.toString().trim();
            if (Strings.isNotEmpty(contentString)) {
                StringSelection contents = new StringSelection(contentString);
                //clip.setContents(contents, null);

                CopyPasteManager copyPasteManager = CopyPasteManager.getInstance();
                copyPasteManager.setContents(contents);
            }

        }
    }
}
