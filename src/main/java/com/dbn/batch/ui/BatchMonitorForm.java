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
import com.dbn.batch.event.BatchEvent;
import com.dbn.batch.event.BatchEventListener;
import com.dbn.batch.event.BatchEventType;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.progress.ProgressForm;
import com.dbn.common.ui.util.UserInterface;
import com.intellij.util.containers.ContainerUtil;
import lombok.Getter;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import java.util.Map;

import static com.dbn.common.ui.Layouts.verticalBoxLayout;

public class BatchMonitorForm extends DBNFormBase implements BatchEventListener {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel tasksPanel;
    private JScrollPane tasksScrollPanel;
    private JPanel progressPanel;

    private final ProgressForm progressForm = new ProgressForm(this);
    private final Map<String, BatchMonitorTaskForm> taskForms = ContainerUtil.createConcurrentWeakValueMap();
    private final @Getter Batch batch;

    public BatchMonitorForm(BatchMonitorDialog dialog) {
        super(dialog);
        initTasksPanel();

        this.batch = dialog.getBatch();
        this.batch.addEventListener(this);

        initHeaderPanel();
        initProgressBar();
        whenShown(() -> startProcess());
    }

    private void initTasksPanel() {
        verticalBoxLayout(tasksPanel);
    }

    private void initHeaderPanel() {
        DBNHeaderForm headerForm = new DBNHeaderForm(this, batch.getContextObject());
        headerPanel.add(headerForm.getMainComponent());
    }

    private void initProgressBar() {
        progressPanel.setVisible(false);
        progressPanel.add(progressForm.getComponent());
        progressForm.setIndeterminate(false);
        progressForm.setMaximum(batch.getInitialTaskCount());
        progressForm.setValue(0);
        progressForm.setText(null);
        progressForm.setText2(null);
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
        Dispatch.run(mainPanel, () -> {
            processEvent(event);
            UserInterface.repaint(mainPanel);
        });
    }

    private void processEvent(BatchEvent event) {
        BatchEventType type = event.getType();
        BatchTask task = event.getTask();
        if (task == null) {
            switch (type) {
                case STARTED: initBatch(); break;
                case FINISHED: competeBatch(); break;
            }
        } else {
            switch (type) {
                case STARTED: initTask(task); break;
                case FINISHED: completeTask(task); break;
            }
        }
    }

    private void competeBatch() {
        progressPanel.setVisible(false);
    }

    private void initBatch() {
        progressPanel.setVisible(true);

        BatchMessenger messenger = batch.getMessenger();
        progressForm.setText(messenger.getBatchProgressMessage(batch));
    }

    private void initTask(BatchTask task) {
        BatchMonitorTaskForm taskForm = new BatchMonitorTaskForm(this, task);
        taskForms.put(task.getIdentifier(), taskForm);
        tasksPanel.add(taskForm.getComponent());

        taskForm.initialize();
        JScrollBar scrollBar = tasksScrollPanel.getVerticalScrollBar();
        scrollBar.revalidate();
        scrollBar.setValue(scrollBar.getMaximum());
    }

    private void completeTask(BatchTask task) {
        String identifier = task.getIdentifier();
        BatchMonitorTaskForm taskForm = taskForms.get(identifier);
        taskForm.complete();
        progressForm.setText2(task.getName());
        progressForm.setValue(batch.getCompletedTaskCount());
    }
}
