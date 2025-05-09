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
import com.dbn.batch.BatchTask;
import com.dbn.batch.event.BatchEvent;
import com.dbn.batch.event.BatchEventListener;
import com.dbn.batch.event.BatchEventType;
import com.dbn.common.message.Message;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.UserInterface;
import com.intellij.util.containers.ContainerUtil;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import java.util.Map;

import static com.dbn.common.ui.Layouts.verticalBoxLayout;

public class BatchMonitorForm extends DBNFormBase implements BatchEventListener {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel tasksPanel;
    private JProgressBar progressBar;
    private JScrollPane tasksScrollPanel;

    private final Map<String, BatchMonitorTaskForm> taskForms = ContainerUtil.createConcurrentWeakValueMap();
    private final Batch batch;

    public BatchMonitorForm(BatchMonitorDialog dialog) {
        super(dialog);
        verticalBoxLayout(tasksPanel);

        this.batch = dialog.getBatch();
        this.batch.addEventListener(this);

        initProgressBar();
        whenShown(() -> startProcess());
    }

    private void initProgressBar() {
        progressBar.setVisible(false);
        progressBar.setIndeterminate(false);
        progressBar.setMaximum(batch.getInitialTaskCount());
        progressBar.setValue(0);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    private void startProcess() {
        Background.run(() -> batch.start());
    }



    @Override
    public void eventOccurred(BatchEvent event) {
        Dispatch.run(getMainComponent(), () -> {
            processEvent(event);
            UserInterface.repaint(getMainComponent());
        });
    }

    private void processEvent(BatchEvent event) {
        BatchEventType type = event.getType();
        BatchTask task = event.getTask();
        if (task == null) {
            switch (type) {
                case STARTED: progressBar.setVisible(true); break;
                case FINISHED: progressBar.setVisible(false); break;
            }
        } else {
            switch (type) {
                case STARTED: addTask(task); break;
                case FINISHED: updateTask(task); break;
            }
        }
    }

    private void updateTask(BatchTask task) {
        Message message = task.getMessage();
        String identifier = task.getIdentifier();
        BatchMonitorTaskForm taskForm = taskForms.get(identifier);

        if (message == null) {
            taskForm.markSuccessful(null);
        } else {
            String messageText = message.getText();
            if (message.isError()) {
                taskForm.markErrored(messageText);
            } else {
                taskForm.markSuccessful(messageText);
            }
        }
        progressBar.setValue(batch.getCompletedTaskCount());
    }

    private void addTask(BatchTask task) {
        BatchMonitorTaskForm form = new BatchMonitorTaskForm(this, task);
        taskForms.put(task.getIdentifier(), form);
        tasksPanel.add(form.getComponent());

        JScrollBar scrollBar = tasksScrollPanel.getVerticalScrollBar();
        scrollBar.setValue(scrollBar.getMaximum());
   }
}
