/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.liquibase.workflows;

import com.dbn.common.action.DataKeys;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.util.Listeners;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.execution.ExecutionResultBase;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.liquibase.workflows.ui.LiquibaseWorkflowResultForm;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.List;

/** Execution-console result aggregating the operation results produced by a Liquibase workflow. */
@Getter
public class LiquibaseWorkflowResult extends ExecutionResultBase<LiquibaseWorkflowResultForm> {
    private final LiquibaseWorkflowContext context;
    private final List<LiquibaseExecutionResult> results = new ArrayList<>();
    private final Listeners<Runnable> listeners = Listeners.create(this);

    public LiquibaseWorkflowResult(@NotNull LiquibaseWorkflowContext context) {
        this.context = context;
    }

    @NotNull
    public List<LiquibaseExecutionResult> getResults() {
        synchronized (results) {
            return new ArrayList<>(results);
        }
    }

    public void addResult(@NotNull LiquibaseExecutionResult result) {
        synchronized (results) {
            results.add(result);
        }
        result.addListener(this::notifyChanged);
        notifyChanged();
    }

    public void addListener(@NotNull Runnable listener) {
        listeners.add(listener);
    }

    public void removeListener(@NotNull Runnable listener) {
        listeners.remove(listener);
    }

    void notifyChanged() {
        listeners.notify(Runnable::run);
    }

    @Nullable
    @Override
    public LiquibaseWorkflowResultForm createForm() {
        return new LiquibaseWorkflowResultForm(this);
    }

    @NotNull
    @Override
    public String getName() {
        return getConnection().getName() + " - " +
                context.getInput().getRelevantSchema().getName() + " - " +
                context.getInput().getWorkflow().getTitle();
    }

    @Override
    public Icon getIcon() {
        return Icons.DB_LIQUIBASE;
    }

    @NotNull
    @Override
    public Project getProject() {
        return context.getInput().getProject();
    }

    @Override
    public ConnectionId getConnectionId() {
        return getConnection().getConnectionId();
    }

    @NotNull
    @Override
    public ConnectionHandler getConnection() {
        return context.getInput().getRelevantConnection();
    }

    @Override
    public DBLanguagePsiFile createPreviewFile() {
        return null;
    }

    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.LIQUIBASE_WORKFLOW_RESULT.is(dataId)) return this;
        return super.getData(dataId);
    }
}
