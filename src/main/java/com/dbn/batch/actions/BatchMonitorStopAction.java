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

package com.dbn.batch.actions;

import com.dbn.batch.Batch;
import com.dbn.batch.ui.BatchMonitorForm;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;
import static com.intellij.icons.AllIcons.Actions.Suspend;

public class BatchMonitorStopAction extends AbstractBatchMonitorAction{
    public BatchMonitorStopAction() {
        super(txt("app.batch.action.Stop"), null, Suspend);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        BatchMonitorForm batchMonitorForm = getBatchMonitorForm(e);
        if (batchMonitorForm == null) return;

        batchMonitorForm.cancelProcess();
    }


    @Override
    public void update(@NotNull AnActionEvent e) {
        Batch batch = getBatch(e);
        boolean visible = batch != null && !batch.isFinished() && !batch.isCancelled();

        Presentation presentation = e.getPresentation();
        presentation.setVisible(visible);
    }
}
