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

package com.dbn.ddl.ui;

import com.dbn.common.file.VirtualFileInfo;
import com.dbn.common.file.ui.FileListCellRenderer;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.object.common.DBSchemaObject;
import org.jetbrains.annotations.NotNull;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;

public class SelectDDLFileForm extends DBNFormBase {
    private JPanel mainPanel;
    private JList<VirtualFileInfo> filesList;
    private JPanel headerPanel;
    private JCheckBox doNotPromptCheckBox;
    private JPanel hintPanel;

    SelectDDLFileForm(DBNDialog<?> parent, DBSchemaObject object, List<VirtualFileInfo> fileInfos, TextContent hint, boolean isFileOpenEvent) {
        super(parent);
        DBNHeaderForm headerForm = new DBNHeaderForm(this, object);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

        DBNHintForm hintForm = new DBNHintForm(this, hint, null, true);
        hintPanel.add(hintForm.getComponent(), BorderLayout.CENTER);

        DefaultListModel<VirtualFileInfo> listModel = new DefaultListModel<>();
        for (VirtualFileInfo fileInfo : fileInfos) {
            listModel.addElement(fileInfo);
        }
        filesList.setModel(listModel);
        filesList.setCellRenderer(new FileListCellRenderer());
        filesList.setSelectedIndex(0);

        if (!isFileOpenEvent) mainPanel.remove(doNotPromptCheckBox);
    }

    public List<VirtualFileInfo> getSelection() {
        return filesList.getSelectedValuesList();
    }

    public void selectAll() {
        filesList.setSelectionInterval(0, filesList.getModel().getSize() -1);
    }

    public void selectNone() {
        filesList.clearSelection();
    }

    public boolean isDoNotPromptSelected() {
        return doNotPromptCheckBox.isSelected();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
