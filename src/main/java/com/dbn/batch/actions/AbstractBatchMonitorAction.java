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
import com.dbn.common.action.BasicAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import static com.dbn.common.action.DataKeys.BATCH_MONITOR_FORM;

public abstract class AbstractBatchMonitorAction extends BasicAction {
    public AbstractBatchMonitorAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    @Nullable
    protected BatchMonitorForm getBatchMonitorForm(@NotNull AnActionEvent e) {
        return e.getData(BATCH_MONITOR_FORM);
    }

    @Nullable
    protected Batch getBatch(AnActionEvent e) {
        BatchMonitorForm form = getBatchMonitorForm(e);
        return form == null ? null : form.getBatch();
    }
}
