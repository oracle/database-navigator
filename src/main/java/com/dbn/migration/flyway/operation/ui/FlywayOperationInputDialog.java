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

package com.dbn.migration.flyway.operation.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.migration.flyway.operation.FlywayOperation;
import com.dbn.migration.flyway.operation.FlywayOperationInput;
import com.dbn.migration.flyway.workspace.FlywayWorkspaceBundle;
import com.dbn.object.DBSchema;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.nls.NlsResources.txt;

/**
 * Input dialog shown before executing a Flyway operation for a database schema.
 */
@Getter
public class FlywayOperationInputDialog extends DBNDialog<FlywayOperationInputForm> {
    private final FlywayOperationInput executionInput;

    public FlywayOperationInputDialog(
            @NotNull DBSchema schema,
            @NotNull FlywayOperation operation,
            @NotNull FlywayWorkspaceBundle workspaces) {
        super(schema.getProject(), txt("app.flyway.title.Operation_" + operation.name()), true);
        executionInput = new FlywayOperationInput(getProject(), operation);
        executionInput.setSchema(schema);
        setDefaultSize(600, 320);
        init();
    }

    @Override
    protected String getDimensionServiceKey() {
        return createDimensionSeviceKey(executionInput.getOperation());
    }

    @NotNull
    @Override
    protected FlywayOperationInputForm createForm() {
        return new FlywayOperationInputForm(this, executionInput);
    }

    @Override
    @NotNull
    protected Action[] initializeActions() {
        renameAction(getOKAction(), txt("msg.flyway.button.Execute_" + executionInput.getOperation().name()));
        return actions(getOKAction(), getCancelAction());
    }

    @Override
    protected void doOKAction() {
        applyFormChanges();
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        applyFormChanges();
        super.doCancelAction();
    }
}
