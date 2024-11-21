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

import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.misc.DBNButton;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Mouse;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.UIUtil;

import javax.swing.AbstractCellEditor;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.io.File;

import static com.dbn.common.ui.util.Mouse.isMainSingleClick;

public class FileBrowserTableCellEditor extends AbstractCellEditor implements TableCellEditor{
    private final JPanel mainPanel = new JPanel();
    private final JTextField textField = new JTextField();
    private final FileChooserDescriptor fileChooserDescriptor;

    public FileBrowserTableCellEditor(FileChooserDescriptor fileChooserDescriptor) {
        this.fileChooserDescriptor = fileChooserDescriptor;
        textField.setBorder(Borders.TEXT_FIELD_INSETS);

        DBNButton button = new DBNButton(Icons.DATA_EDITOR_BROWSE);
        button.setMaximumSize(new Dimension(10, -1));
        mainPanel.setBackground(UIUtil.getTableBackground());
        button.addMouseListener(Mouse.listener().onClick(e -> openFileChooser(e)));

        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(textField, BorderLayout.CENTER);
        mainPanel.add(button, BorderLayout.EAST);

        FileChooserFactory.getInstance().installFileCompletion(textField, fileChooserDescriptor, true, null);
    }

    private void openFileChooser(MouseEvent e) {
        if (!isMainSingleClick(e)) return;

        FileChooserDialog fileChooser = FileChooserFactory.getInstance().createFileChooser(fileChooserDescriptor, null, null);
        VirtualFile file = LocalFileSystem.getInstance().findFileByIoFile(new File(textField.getText()));
        VirtualFile[] virtualFiles = fileChooser.choose(null, file);
        if (virtualFiles.length > 0) {
            textField.setText(new File(virtualFiles[0].getPath()).getPath());
        }
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        textField.setText((String) value);
        Dispatch.run(() -> {
            textField.selectAll();
            textField.requestFocus();
        });
        return mainPanel;
    }

    @Override
    public Object getCellEditorValue() {
        return textField.getText();
    }
}
