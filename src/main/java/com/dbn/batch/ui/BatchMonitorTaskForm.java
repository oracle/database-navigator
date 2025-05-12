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

import com.dbn.batch.Batch;
import com.dbn.batch.BatchMessenger;
import com.dbn.batch.BatchTask;
import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.form.DBNFormBase;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.Color;

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
        messageTextPane.setForeground(getMessageColor(false));
    }

    private Batch getBatch() {
        BatchMonitorForm parentForm = ensureParentComponent();
        return parentForm.getBatch();
    }

    private BatchMessenger getMessenger() {
        return getBatch().getMessenger();
    }

    public void initialize() {
        statusPanel.removeAll();
        statusPanel.add(new AsyncProcessIcon("Processing..."));

        Batch batch = getBatch();
        BatchMessenger messenger = getMessenger();
        String message = messenger.createTaskInitMessage(batch, task);
        messageTextPane.setText(message);
    }

    public void complete() {
        statusPanel.removeAll();
        Exception exception = task.getException();

        Batch batch = getBatch();
        BatchMessenger messenger = getMessenger();
        String message = exception == null ?
                messenger.createTaskSuccessMessage(batch, task) :
                messenger.createTaskErrorMessage(batch, task, exception);


        Icon icon = exception == null ?
                Icons.COMMON_STATUS_SUCCESS :
                Icons.COMMON_STATUS_ERROR;

        Color foreground = getMessageColor(exception != null);

        statusPanel.add(new JLabel(icon));
        messageTextPane.setText(message);
        messageTextPane.setForeground(foreground);

    }

    private static @NotNull Color getMessageColor(boolean error) {
        return error ?
                Colors.dimmer(UIUtil.getErrorForeground()) :
                Colors.faded(UIUtil.getLabelForeground());
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
