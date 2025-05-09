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

package com.dbn.sync.java.upload.ui;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.sync.java.upload.JavaUploadBatch;
import com.dbn.sync.java.upload.JavaUploadInput;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;

import javax.swing.JComponent;
import javax.swing.JPanel;

public class JavaUploadResultForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel filesPanel;
    private JBList<String> fileList;

    public JavaUploadResultForm(JavaUploadResultDialog dialog, JavaUploadBatch batch) {
        super(dialog);

        initHeaderPanel(batch);
        initHintPanel(batch);
        initObjectList(batch);
    }

    private void initHeaderPanel(JavaUploadBatch batch) {
        DBNHeaderForm headerForm = new DBNHeaderForm(this, batch.getInput().getRootFile());
        this.headerPanel.add(headerForm.getMainComponent());
    }

    private void initHintPanel(JavaUploadBatch batch) {
        JavaUploadInput input = batch.getInput();
        TextContent hintText = TextContent.plain("The following classes were created or updated in your \n Connection " + input.getTargetConnection().getName() +
                " \n Schema " + input.getTargetSchemaName());
        DBNHintForm hintForm = new DBNHintForm(this, hintText, null, true);
        hintPanel.add(hintForm.getComponent());
    }


    private void initObjectList(JavaUploadBatch batch) {
        fileList.setModel(new CollectionListModel<>(batch.getUploadedFiles()));
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
