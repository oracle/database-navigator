/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.data.editor.ui;

import com.dbn.common.icon.Icons;
import com.dbn.common.ui.misc.DBNButton;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Mouse;
import com.dbn.data.value.NestedTableValue;
import com.dbn.editor.data.model.DatasetEditorModelCell;
import com.dbn.editor.data.nested.DBNestedTableViewerDialog;
import com.dbn.editor.data.ui.table.DatasetEditorTable;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

import static com.dbn.nls.NlsResources.txt;

public final class TextFieldWithTableEditor extends TextFieldWithButtons {
    private final JComponent button;

    public TextFieldWithTableEditor(DatasetEditorTable table) {
        super(table.getProject());

        button = createButton(Icons.DATA_EDITOR_BROWSE, txt("app.objects.action.ViewData"));
        button.setBorder(Borders.insetBorder(1));
        button.setOpaque(false);
        button.setToolTipText(txt("app.objects.action.ViewData"));
        button.addMouseListener(Mouse.listener().onClick(e -> openEditor()));
        add(button, BorderLayout.EAST);

        int rowHeight = table.getRowHeight();
        button.setPreferredSize(new Dimension(Math.max(20, rowHeight), rowHeight - 2));
        table.addPropertyChangeListener(e -> {
            Object newProperty = e.getNewValue();
            if (newProperty instanceof Font) {
                int rowHeight1 = table.getRowHeight();
                button.setPreferredSize(new Dimension(Math.max(20, rowHeight1), rowHeight1 - 2));
            }
        });
    }

    @Override
    public JComponent createButton(Icon icon, String name) {
        return new DBNButton(icon, name);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        button.setEnabled(enabled);
    }

    @Override
    public void setEditable(boolean editable) {
        super.setEditable(editable);
        JTextField textField = getTextField();
        setBackground(textField.getBackground());
        button.setBackground(textField.getBackground());
    }

    public void openEditor() {
        if (getUserValueHolder() instanceof DatasetEditorModelCell cell &&
                cell.getUserValue() instanceof NestedTableValue nestedTableValue) {
            DBNestedTableViewerDialog.show(nestedTableValue, cell);
        }
    }
}
