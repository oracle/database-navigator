/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.execution.script.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.execution.script.ScriptExecutionInput;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

public class ScriptExecutionInputDialog extends DBNDialog<ScriptExecutionInputForm> {
    private ScriptExecutionInput executionInput;

    public ScriptExecutionInputDialog(Project project, ScriptExecutionInput executionInput) {
        super(project, "Execute SQL script", true);
        this.executionInput = executionInput;
        setModal(true);
        renameAction(getOKAction(), "Execute");
        init();
    }

    @NotNull
    @Override
    protected ScriptExecutionInputForm createForm() {
        return new ScriptExecutionInputForm(this, executionInput);
    }

    @Override
    protected String getDimensionServiceKey() {
        return null;
    }

    @Override
    @NotNull
    protected final Action[] createActions() {
        return new Action[]{
                getOKAction(),
                getCancelAction(),
        };
    }

    public void setActionEnabled(boolean enabled) {
        getOKAction().setEnabled(enabled);
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
    }
}
