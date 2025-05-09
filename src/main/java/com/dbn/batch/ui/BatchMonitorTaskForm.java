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

package com.dbn.batch.ui;

import com.dbn.batch.BatchTask;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.form.DBNFormBase;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.UIUtil;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;

public class BatchMonitorTaskForm extends DBNFormBase {

    private JPanel mainPanel;
    private JPanel statusPanel;
    private JLabel titleLabel;
    private JTextPane messageTextPane;


    private final BatchTask task;

    public BatchMonitorTaskForm(BatchMonitorForm parent, BatchTask task) {
        super(parent);
        this.task = task;

        titleLabel.setText(task.getName());
        titleLabel.setIcon(task.getIcon());
        messageTextPane.setForeground(UIUtil.getLabelDisabledForeground());

        markRunning();
    }


    public void markRunning() {
        statusPanel.removeAll();
        statusPanel.add(new AsyncProcessIcon("Processing..."));
    }

    public void markSuccessful(String message) {
        statusPanel.removeAll();
        statusPanel.add(new JLabel(Icons.COMMON_STATUS_SUCCESS));
        messageTextPane.setText(message);
    }

    public void markErrored(String message) {
        statusPanel.removeAll();
        statusPanel.add(new JLabel(Icons.COMMON_STATUS_ERROR));
        messageTextPane.setText(message);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
