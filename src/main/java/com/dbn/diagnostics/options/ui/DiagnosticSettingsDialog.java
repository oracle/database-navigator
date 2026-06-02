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
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.common.util.Messages.showErrorDialog;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

public class DiagnosticSettingsDialog extends DBNDialog<DiagnosticSettingsForm> {

    public DiagnosticSettingsDialog(Project project) {
        super(project, txt("msg.diagnostics.title.DiagnosticSettings"), true);
        setModal(false);
        setResizable(true);
        setCancelButtonText("Cancel");
        setDefaultSize(600, 800);
        init();
    }

    @NotNull
    @Override
    protected DiagnosticSettingsForm createForm() {
        return new DiagnosticSettingsForm(this);
    }

    @Override
    @NotNull
    protected final Action[] initializeActions() {
        return actions(
                getOKAction(),
                getCancelAction());
    }

    @Override
    protected void doOKAction() {
        DiagnosticSettingsForm settingsForm = getForm();
        try {
            settingsForm.applyFormChanges();
            super.doOKAction();
        } catch (ConfigurationException e) {
            conditionallyLog(e);
            showErrorDialog(getProject(), txt("msg.diagnostics.title.InvalidConfiguration"), e.getMessage());
        }

    }
}
