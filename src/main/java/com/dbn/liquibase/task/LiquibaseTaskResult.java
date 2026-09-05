/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.liquibase.task;

import com.dbn.common.icon.Icons;
import com.dbn.common.task.TaskStatus;
import com.dbn.common.ui.util.Listeners;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.execution.ExecutionCancellationAdapter;
import com.dbn.execution.ExecutionResultBase;
import com.dbn.execution.common.result.ui.ExecutionResultForm;
import com.dbn.language.common.DBLanguagePsiFile;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/** Base execution result shared by Liquibase operation and workflow tasks. */
@Getter
public abstract class LiquibaseTaskResult<
        I extends LiquibaseTaskInput,
        C extends LiquibaseTaskContext<I>,
        F extends ExecutionResultForm>
        extends ExecutionResultBase<F> {

    private final C context;
    private final Listeners<Runnable> listeners = Listeners.create(this);

    protected LiquibaseTaskResult(@NotNull C context) {
        this.context = context;
    }

    @NotNull
    public I getInput() {
        return context.getInput();
    }

    public final TaskStatus getStatus() {
        return context.getStatus();
    }

    @Override
    public ExecutionCancellationAdapter getCancellationAdapter() {
        return getStatus() == TaskStatus.RUNNING
                ? new LiquibaseTaskCancellationAdapter(this)
                : null;
    }

    public abstract boolean canRerun();

    public void addListener(@NotNull Runnable listener) {
        listeners.add(listener);
    }

    public void removeListener(@NotNull Runnable listener) {
        listeners.remove(listener);
    }

    public void notifyChanged() {
        listeners.notify(Runnable::run);
    }

    public Project getProject(){
        return getInput().getProject();
    }

    @Override
    public Icon getIcon() {
        return Icons.DB_LIQUIBASE;
    }

    @Override
    public ConnectionId getConnectionId() {
        return getConnection().getConnectionId();
    }

    @NotNull
    @Override
    public ConnectionHandler getConnection() {
        return getInput().getRelevantConnection();
    }

    @Override
    public DBLanguagePsiFile createPreviewFile() {
        return null;
    }
}
