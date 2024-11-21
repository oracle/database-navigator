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

package com.dbn.execution.method.history.ui;

import com.dbn.common.icon.Icons;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.database.DatabaseFeature;
import com.dbn.debugger.DatabaseDebuggerManager;
import com.dbn.execution.method.MethodExecutionInput;
import com.dbn.execution.method.MethodExecutionManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;

public class MethodExecutionHistoryDialog extends DBNDialog<MethodExecutionHistoryForm> {
    private SelectAction selectAction;
    private ExecuteAction executeAction;
    private DebugAction debugAction;
    private SaveAction saveAction;
    private CloseAction closeAction;
    private final boolean editable;
    private final boolean debug;
    private WeakRef<MethodExecutionInput> selectedExecutionInput;  // TODO dialog result - Disposable.nullify(...)

    public MethodExecutionHistoryDialog(
            @NotNull Project project,
            @Nullable MethodExecutionInput executionInput,
            boolean editable,
            boolean modal,
            boolean debug) {

        super(project, "Method execution history", true);
        this.selectedExecutionInput = WeakRef.of(executionInput);
        this.editable = editable;
        this.debug = debug;
        setModal(modal);
        setResizable(true);
        setDefaultSize(1200, 800);
        init();

        updateMainButtons(executionInput);
    }

    @NotNull
    @Override
    protected MethodExecutionHistoryForm createForm() {
        return new MethodExecutionHistoryForm(this, WeakRef.get(selectedExecutionInput), debug);
    }

    @Override
    @NotNull
    protected final Action[] createActions() {
        if (editable) {
            executeAction = new ExecuteAction();
            executeAction.setEnabled(false);
            debugAction = new DebugAction();
            debugAction.setEnabled(false);
            saveAction = new SaveAction();
            saveAction.setEnabled(false);
            closeAction = new CloseAction();
            return new Action[]{executeAction, debugAction, saveAction, closeAction};
        } else {
            selectAction = new SelectAction();
            selectAction.setEnabled(false);
            closeAction = new CloseAction();
            renameAction(closeAction, "Cancel");
            return new Action[]{selectAction, closeAction};
        }
    }

    public boolean isEditable() {
        return editable;
    }

    private void saveChanges() {
        MethodExecutionHistoryForm component = getForm();
        component.updateMethodExecutionInputs();
        MethodExecutionManager methodExecutionManager = MethodExecutionManager.getInstance(getProject());
        methodExecutionManager.setExecutionInputs(component.getExecutionInputs());
    }

    public void setSelectedExecutionInput(MethodExecutionInput selectedExecutionInput) {
        this.selectedExecutionInput = WeakRef.of(selectedExecutionInput);
    }

    @Nullable
    public MethodExecutionInput getSelectedExecutionInput() {
        return WeakRef.get(selectedExecutionInput);
    }

    /**********************************************************
     *                         Actions                        *
     **********************************************************/
    private class SelectAction extends AbstractAction {
        SelectAction() {
            super("Select");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            saveChanges();
            close(OK_EXIT_CODE);
        }
    }

    private class ExecuteAction extends AbstractAction {
        ExecuteAction() {
            super("Execute", Icons.METHOD_EXECUTION_RUN);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            saveChanges();
            MethodExecutionInput executionInput = getForm().getTree().getSelectedExecutionInput();
            if (executionInput != null) {
                MethodExecutionManager executionManager = MethodExecutionManager.getInstance(getProject());
                close(OK_EXIT_CODE);
                executionManager.execute(executionInput);
            }
        }
    }

    private class DebugAction extends AbstractAction {
        DebugAction() {
            super("Debug", Icons.METHOD_EXECUTION_DEBUG);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            saveChanges();
            MethodExecutionInput executionInput = getForm().getTree().getSelectedExecutionInput();
            if (executionInput != null) {
                DatabaseDebuggerManager debuggerManager = DatabaseDebuggerManager.getInstance(getProject());
                close(OK_EXIT_CODE);
                debuggerManager.startMethodDebugger(executionInput.getMethod());
            }
        }
    }

    private class SaveAction extends AbstractAction {
        SaveAction() {
            super("Save");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            saveChanges();
            saveAction.setEnabled(false);
            renameAction(closeAction, "Close");
        }
    }

    private class CloseAction extends AbstractAction {
        CloseAction() {
            super("Close");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            doCancelAction();
        }
    }

    void updateMainButtons(MethodExecutionInput selection){
        if (selection == null) {
            if (executeAction != null) executeAction.setEnabled(false);
            if (debugAction != null) debugAction.setEnabled(false);
            if (selectAction != null) selectAction.setEnabled(false);
        } else {
            if (executeAction != null) executeAction.setEnabled(true);
            if (debugAction != null) debugAction.setEnabled(DatabaseFeature.DEBUGGING.isSupported(selection));
            if (selectAction != null) selectAction.setEnabled(true);
        }
    }

    void setSaveButtonEnabled(boolean enabled){
        if (isDisposed()) return;

        if (saveAction != null) saveAction.setEnabled(enabled);
        renameAction(closeAction, enabled ? "Cancel" : "Close");
    }
}
