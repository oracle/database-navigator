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
import com.dbn.sync.java.download.JavaDownloadContext;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBList;

import javax.swing.JComponent;
import javax.swing.JPanel;

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
        DBNHeaderForm headerForm = new DBNHeaderForm(this, context.getInput().getJavaClass());
        this.headerPanel.add(headerForm.getMainComponent());
    }

    private void initHintPanel(JavaDownloadContext context) {
        VirtualFile rootDirectory = context.getTargetRootDirectory();
        TextContent hintText = TextContent.plain("The following classes were created or updated in your project under " + rootDirectory.getPath());
        DBNHintForm hintForm = new DBNHintForm(this, hintText, null, true);
        hintPanel.add(hintForm.getComponent());
    }


    private void initObjectList(JavaDownloadContext context) {
        fileList.setModel(VirtualFileListModel.create(this, context.getDownloadedFiles()));
        fileList.setCellRenderer(VirtualFileListCellRenderer.create());
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
