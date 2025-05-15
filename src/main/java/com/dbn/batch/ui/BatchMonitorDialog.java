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
import com.dbn.batch.event.BatchEvent;
import com.dbn.batch.event.BatchEventListener;
import com.dbn.batch.event.BatchEventType;
import com.dbn.common.ui.dialog.DBNDialog;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

@Getter
public class BatchMonitorDialog extends DBNDialog<BatchMonitorForm> implements BatchEventListener {
    private final Batch batch;

    public BatchMonitorDialog(Batch batch) {
        super(batch.getProject(), batch.getMessenger().getBatchTitle(batch), false);
        this.setModal(false);
        this.batch = batch;
        this.batch.addEventListener(this);
        setDefaultSize(600, 600);
        renameAction(getCancelAction(), "Close");
        init();
    }

    @NotNull
    @Override
    protected BatchMonitorForm createForm() {
        return new BatchMonitorForm(this);
    }

    @Override
    protected final Action @NotNull [] createActions() {
        return createActions(getCancelAction());
    }

    @Override
    public void doCancelAction() {
        if (batch.isCancelled() || batch.isFinished()) {
            super.doCancelAction();
        }
    }

    @Override
    public void eventOccurred(BatchEvent event) {
        if (event.getTask() != null) return;

        BatchEventType type = event.getType();
        Action cancelAction = getCancelAction();

        switch (type) {
            case STARTED: cancelAction.setEnabled(false); break;
            case FINISHED: cancelAction.setEnabled(true); break;
            case CANCELLED: cancelAction.setEnabled(true); break;
        }
    }
}