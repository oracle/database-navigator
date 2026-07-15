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

package com.dbn.liquibase.execution.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.execution.LiquibaseOperationSupport;
import com.dbn.liquibase.model.LiquibaseWorkspaceBundle;
import com.dbn.object.DBSchema;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.liquibase.execution.LiquibaseExecutionProcessor.confirmOverwrite;
import static com.dbn.liquibase.execution.LiquibaseOperation.GENERATE_CHANGELOG;
import static com.dbn.liquibase.execution.LiquibaseOperation.GENERATE_DIFF_CHANGELOG;
import static com.dbn.nls.NlsResources.txt;

/** Input dialog shown before executing a Liquibase operation for a database schema. */
@Getter
public class LiquibaseExecutionInputDialog extends DBNDialog<LiquibaseExecutionInputForm> {
    private final LiquibaseWorkspaceBundle workspaces;
    private final LiquibaseExecutionInput executionInput;

    public LiquibaseExecutionInputDialog(
            @NotNull DBSchema schema,
            @NotNull LiquibaseOperation operation,
            @NotNull LiquibaseWorkspaceBundle workspaces) {
        super(schema.getProject(), txt("cfg.liquibase.title.Operation_" + operation.name()), true);
        this.executionInput = new LiquibaseExecutionInput(getProject(), operation);
        this.workspaces = workspaces;
        LiquibaseOperationSupport support = operation.getSupport();
        if (support.requiresSourceSchema()) executionInput.setSourceSchema(schema);
        if (support.requiresTargetSchema() && !support.requiresSourceSchema()) {
            executionInput.setTargetSchema(schema);
        }
        setDefaultSize(600, 320);
        init();
    }

    @Override
    protected String getDimensionServiceKey() {
        return createDimensionSeviceKey(executionInput.getOperation());
    }

    @NotNull
    @Override
    protected LiquibaseExecutionInputForm createForm() {
        return new LiquibaseExecutionInputForm(this);
    }

    @Override
    @NotNull
    protected Action[] initializeActions() {
        renameAction(getOKAction(), txt("msg.liquibase.button.Execute_" + executionInput.getOperation().name()));
        return actions(getOKAction(), getCancelAction());
    }

    @Override
    protected void doOKAction() {
        applyFormChanges();
        LiquibaseOperation operation = executionInput.getOperation();
        boolean checkOverride = operation.isOneOf(
                GENERATE_CHANGELOG,
                GENERATE_DIFF_CHANGELOG);
        if (checkOverride && !confirmOverwrite(executionInput)) {
            return;
        }
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        applyFormChanges(); // preserve input even if canceled
        super.doCancelAction();
    }
}
