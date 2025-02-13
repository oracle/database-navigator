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

package com.dbn.execution.java.ui;

import com.dbn.common.icon.Icons;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.diagnostics.Diagnostics;
import com.dbn.execution.java.JavaExecutionInput;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;

public class JavaExecutionInputDialog extends DBNDialog<JavaExecutionInputForm> {
    private final JavaExecutionInput executionInput;
    private final DBDebuggerType debuggerType;
    private final Runnable executor;

    public JavaExecutionInputDialog(@NotNull JavaExecutionInput executionInput, @NotNull DBDebuggerType debuggerType, @NotNull Runnable executor) {
        super(executionInput.getProject(), (debuggerType.isDebug() ? "Debug" : "Execute") + " method", true);
        this.executionInput = executionInput;
        this.debuggerType = debuggerType;
        this.executor = executor;
        setModal(false);
        setResizable(true);
        setDefaultSize(800, 600);
        init();
    }

    @NotNull
    @Override
    protected JavaExecutionInputForm createForm() {
        return new JavaExecutionInputForm(this, executionInput, debuggerType, true);
    }

    @Override
    @NotNull
    protected final Action[] createActions() {
        return new Action[]{
                new ExecuteAction(),
                getCancelAction(),
                getHelpAction()
        };
    }

    @Override
    protected String getDimensionServiceKey() {
        // remember dialog dimension based on number of fields
        return Diagnostics.isDialogSizingReset() ? null : super.getDimensionServiceKey() + getForm().countFields();
    }

    private class ExecuteAction extends AbstractAction {
        ExecuteAction() {
            super(debuggerType.isDebug() ? "Debug" : "Execute",
                    debuggerType.isDebug() ? Icons.METHOD_EXECUTION_DEBUG : Icons.METHOD_EXECUTION_RUN);
            makeFocusAction(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                getForm().updateExecutionInput();
                executor.run();
            } finally {
                doOKAction();
            }
        }
    }
}
