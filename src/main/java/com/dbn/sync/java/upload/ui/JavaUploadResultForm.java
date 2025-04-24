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
import com.dbn.sync.java.upload.JavaUploadContext;
import com.dbn.sync.java.upload.JavaUploadInput;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.util.List;

public class JavaUploadResultForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel filesPanel;
    private JBList<String> fileList;
    private JPanel errorsPanel;
    private JTable errorsTable;

    public JavaUploadResultForm(JavaUploadResultDialog dialog, JavaUploadContext context) {
        super(dialog);

        initHeaderPanel(context);
        initHintPanel(context);
        initObjectList(context);
		initErrorsTable(context.getErrors());
    }

    private void initHeaderPanel(JavaUploadContext context) {
        DBNHeaderForm headerForm = new DBNHeaderForm(this, context.getInput().getJavaClass());
        this.headerPanel.add(headerForm.getMainComponent());
    }

    private void initHintPanel(JavaUploadContext context) {
        JavaUploadInput input = context.getInput();
        TextContent hintText = TextContent.plain("The following classes were created or updated in your \n Connection " + input.getConnection().getName() +
                " \n Schema " + input.getSchemaName());
        DBNHintForm hintForm = new DBNHintForm(this, hintText, null, true);
        hintPanel.add(hintForm.getComponent());
    }


    private void initObjectList(JavaUploadContext context) {
        fileList.setModel(new CollectionListModel<>(context.getUploadedFiles()));
    }

    private void initErrorsTable(List<List<String>> errors) {
        if(errors.isEmpty()){
            return;
        }

        Object[][] tableData = new Object[errors.size()][2];
        for (int i = 0; i < errors.size(); i++) {
            List<String> row = errors.get(i);
            tableData[i][0] = row.get(0);
            tableData[i][1] = row.get(1);
        }

        // Set column names
        String[] columnNames = {"Class Name", "Error"};

        // Set model
        DefaultTableModel model = new DefaultTableModel(tableData, columnNames);
        errorsTable.setModel(model);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
