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
import com.dbn.execution.script.CmdLineInterface;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.util.Set;

public class CmdLineInterfaceInputDialog extends DBNDialog<CmdLineInterfaceInputForm> {
    private final CmdLineInterface cmdLineInterface;
    private final Set<String> usedNames;

    public CmdLineInterfaceInputDialog(Project project, @NotNull CmdLineInterface cmdLineInterface, @NotNull Set<String> usedNames) {
        super(project, "Add command-line interface", true);
        this.cmdLineInterface = cmdLineInterface;
        this.usedNames = usedNames;
        setModal(true);
        renameAction(getOKAction(), "Save");
        init();
    }

    @NotNull
    @Override
    protected CmdLineInterfaceInputForm createForm() {
        return new CmdLineInterfaceInputForm(this, cmdLineInterface, usedNames);
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

    void setActionEnabled(boolean enabled) {
        getOKAction().setEnabled(enabled);
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
    }
}
