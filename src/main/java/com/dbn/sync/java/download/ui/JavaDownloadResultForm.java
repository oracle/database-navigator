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

package com.dbn.sync.java.download.ui;

import com.dbn.common.file.ui.VirtualFileListCellRenderer;
import com.dbn.common.file.ui.VirtualFileListModel;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.util.Editors;
import com.dbn.sync.java.download.JavaDownloadContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBList;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;

import static com.dbn.common.ui.util.Keyboard.onKeyPress;
import static com.dbn.common.ui.util.Lists.onSelectionChange;
import static com.dbn.common.ui.util.Mouse.onMouseClick;
import static java.awt.event.MouseEvent.BUTTON1;

public class JavaDownloadResultForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel filesPanel;
    private JBList<VirtualFile> fileList;

    public JavaDownloadResultForm(JavaDownloadResultDialog dialog, JavaDownloadContext context) {
        super(dialog);

        initHeaderPanel(context);
        initHintPanel(context);
        initObjectList(context);
    }

    private void initHeaderPanel(JavaDownloadContext context) {
        DBNHeaderForm headerForm = new DBNHeaderForm(this, context.getInput().getSourceObject());
        this.headerPanel.add(headerForm.getMainComponent());
    }

    private void initHintPanel(JavaDownloadContext context) {
        VirtualFile rootDirectory = context.getTargetRootDirectory();
        TextContent hintText = TextContent.plain(
                "The following classes were created or updated in your project under " + rootDirectory.getPath() + "\n\n" +
                        "(double click on the files, or press Enter to open them in the editor)");
        DBNHintForm hintForm = new DBNHintForm(this, hintText, null, true);
        hintPanel.add(hintForm.getComponent());
    }


    private void initObjectList(JavaDownloadContext context) {
        fileList.setModel(VirtualFileListModel.create(this, context.getDownloadedFiles()));
        fileList.setCellRenderer(VirtualFileListCellRenderer.create());

        onMouseClick(fileList, BUTTON1, 2, e -> openJavaEditor(e));
        onKeyPress(fileList, KeyEvent.VK_ENTER, e -> openJavaEditors(true));
        onSelectionChange(fileList, e -> updateDialogButtons());
    }

    private void updateDialogButtons() {
        JavaDownloadResultDialog dialog = ensureParentComponent();
        dialog.getOpenSelectedAction().setEnabled(fileList.getSelectedIndices().length > 0);
    }

    private void openJavaEditor(MouseEvent e) {
        int rowNumber = fileList.locationToIndex(e.getPoint());
        if (rowNumber < 0) return;

        VirtualFile file = fileList.getModel().getElementAt(rowNumber);
        if (file == null) return;

        Project project = getProject();
        Editors.openFileEditor(project, file, false);
    }

    protected void openJavaEditors(boolean selected) {
        Project project = getProject();
        List<VirtualFile> files = getJavaFiles(selected);
        for (VirtualFile file : files) {
            Editors.openFileEditor(project, file, false);
        }
    }

    private List<VirtualFile> getJavaFiles(boolean selected) {
        if (selected) return fileList.getSelectedValuesList();

        VirtualFileListModel model = (VirtualFileListModel) fileList.getModel();
        return model.getElements();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
