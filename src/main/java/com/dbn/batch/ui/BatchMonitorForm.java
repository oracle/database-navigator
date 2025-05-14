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
import com.dbn.common.action.DataKeys;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.messages.DBNMessageForm;
import com.dbn.common.ui.progress.ProgressForm;
import com.dbn.common.ui.util.UserInterface;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.util.containers.ContainerUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import java.util.Map;

import static com.dbn.common.icon.Icons.DIALOG_SUCCESS;
import static com.dbn.common.icon.Icons.DIALOG_WARNING;
import static com.dbn.common.ui.Layouts.verticalBoxLayout;
import static com.dbn.common.util.Actions.createActionToolbar;

@SuppressWarnings("unchecked")
public class BatchMonitorForm extends DBNFormBase implements BatchEventListener {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel tasksPanel;
    private JScrollPane tasksScrollPanel;
    private JPanel progressPanel;
    private JPanel actionsPanel;
    private JPanel messagePanel;

    private final ProgressForm progressForm = new ProgressForm(this);
    private final Map<String, BatchMonitorTaskForm> taskForms = ContainerUtil.createConcurrentWeakValueMap();
    private final @Getter Batch batch;

    public BatchMonitorForm(BatchMonitorDialog dialog) {
        super(dialog);
        this.batch = dialog.getBatch();
        this.batch.addEventListener(this);

        initHeaderPanel();
        initProgressBar();
        initProgressActions();
        initMessagePanel();
        initTasksPanel();
        whenShown(() -> batch.start());
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

    private void initProgressActions() {
        ActionToolbar actionToolbar = createActionToolbar(actionsPanel, true, "DBNavigator.ActionGroup.BatchMonitor.Controls");
        actionsPanel.add(actionToolbar.getComponent());
    }

    private void initMessagePanel() {
        messagePanel.setVisible(false);
    }

    private void showMessagePanel() {
        BatchMessenger messenger = batch.getMessenger();
        String title = messenger.getBatchProgressTitle(batch);
        String message = messenger.getBatchProgressDetail(batch, null);

        Icon icon = batch.isCancelled() ? DIALOG_WARNING : DIALOG_SUCCESS;


        DBNMessageForm messageForm = new DBNMessageForm(this, icon, title, message);
        messagePanel.add(messageForm.getComponent());
        messagePanel.setVisible(true);
    }

    private void hideProgressPanel() {
        progressForm.setText("");
        progressForm.setText2("");
        progressPanel.setVisible(false);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public void cancelProcess() {
        batch.cancel();
    }

    public void pauseProcess() {
        batch.pause();
    }

    public void resumeProcess() {
        batch.resume();
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
                case STARTED: onBatchStarted(); break;
                case PAUSED: onBatchPause(); break;
                case RESUMED: onBatchResume(); break;
                case CANCELLED: onBatchCancel(); break;
                case FINISHED: onBatchCompletion(); break;
            }
        } else {
            switch (type) {
                case STARTED: onTaskStart(task); break;
                case FINISHED: onTaskCompletion(task); break;
            }
        }
    }

    private void onBatchStarted() {
        progressPanel.setVisible(true);

        BatchMessenger messenger = batch.getMessenger();
        String progressTitle = messenger.getBatchProgressTitle(batch);
        progressForm.setText(progressTitle);
    }

    private void onBatchPause() {
        BatchMessenger messenger = batch.getMessenger();
        progressForm.setText(messenger.getBatchProgressTitle(batch));
        progressForm.setText2(messenger.getBatchProgressDetail(batch, null));
    }

    private void onBatchResume() {

    }

    private void onBatchCancel() {
        hideProgressPanel();
        showMessagePanel();
    }

    private void onBatchCompletion() {
        hideProgressPanel();
        showMessagePanel();
    }

    private void onTaskStart(BatchTask task) {

        BatchMonitorTaskForm taskForm = new BatchMonitorTaskForm(this, task);
        taskForms.put(task.getIdentifier(), taskForm);
        tasksPanel.add(taskForm.getComponent());

        BatchMessenger messenger = batch.getMessenger();
        String progressDetail = messenger.getBatchProgressDetail(batch, task);
        progressForm.setText2(progressDetail);

        taskForm.initialize();
        JScrollBar scrollBar = tasksScrollPanel.getVerticalScrollBar();
        scrollBar.revalidate();
        scrollBar.setValue(scrollBar.getMaximum());
    }

    private void onTaskCompletion(BatchTask task) {
        String identifier = task.getIdentifier();
        BatchMonitorTaskForm taskForm = taskForms.get(identifier);
        taskForm.complete();
        progressForm.setValue(batch.getCompletedTaskCount());
    }

    public Object getData(@NotNull String dataId) {
        if (DataKeys.BATCH_MONITOR_FORM.is(dataId)) return this;
        return null;
    }
}
