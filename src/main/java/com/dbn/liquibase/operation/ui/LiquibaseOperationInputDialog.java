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

package com.dbn.liquibase.operation.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.liquibase.operation.LiquibaseOperation;
import com.dbn.liquibase.operation.LiquibaseOperationInput;
import com.dbn.liquibase.workspace.LiquibaseWorkspaceBundle;
import com.dbn.object.DBSchema;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.liquibase.operation.LiquibaseFeature.SOURCE_SCHEMA;
import static com.dbn.liquibase.operation.LiquibaseFeature.TARGET_SCHEMA;
import static com.dbn.liquibase.operation.LiquibaseOperationConfirmations.confirm;
import static com.dbn.nls.NlsResources.txt;

/** Input dialog shown before executing a Liquibase operation for a database schema. */
@Getter
public class LiquibaseOperationInputDialog extends DBNDialog<LiquibaseOperationInputForm> {
    private final LiquibaseWorkspaceBundle workspaces;
    private final LiquibaseOperationInput executionInput;

    public LiquibaseOperationInputDialog(
            @NotNull DBSchema schema,
            @NotNull LiquibaseOperation operation,
            @NotNull LiquibaseWorkspaceBundle workspaces) {
        super(schema.getProject(), txt("app.liquibase.title.Operation_" + operation.name()), true);
        this.executionInput = new LiquibaseOperationInput(getProject(), operation);
        this.workspaces = workspaces;
        if (operation.requires(SOURCE_SCHEMA)) {
            executionInput.setSourceSchema(schema);
        }
        if (operation.requires(TARGET_SCHEMA) && !operation.requires(SOURCE_SCHEMA)) {
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
    protected LiquibaseOperationInputForm createForm() {
        return new LiquibaseOperationInputForm(this);
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
        if (!confirm(executionInput)) return;
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        applyFormChanges(); // preserve input even if canceled
        super.doCancelAction();
    }
}
