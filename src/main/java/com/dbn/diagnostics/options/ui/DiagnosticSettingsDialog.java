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

package com.dbn.diagnostics.options.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Messages;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class DiagnosticSettingsDialog extends DBNDialog<DiagnosticSettingsForm> {

    public DiagnosticSettingsDialog(Project project) {
        super(project, "Diagnostic Settings", true);
        setModal(false);
        setResizable(true);
        setCancelButtonText("Cancel");
        init();
    }

    @NotNull
    @Override
    protected DiagnosticSettingsForm createForm() {
        return new DiagnosticSettingsForm(this);
    }

    @Override
    @NotNull
    protected final Action[] createActions() {
        return new Action[]{
                getOKAction(),
                getCancelAction(),
                getHelpAction()
        };
    }

    @Override
    protected void doOKAction() {
        DiagnosticSettingsForm settingsForm = getForm();
        try {
            settingsForm.applyFormChanges();
            super.doOKAction();
        } catch (ConfigurationException e) {
            conditionallyLog(e);
            Messages.showErrorDialog(getProject(), "Invalid Configuration", e.getMessage());
        }

    }
}
