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

package com.dbn.diagnostics.action;

import com.dbn.common.action.ProjectAction;
import com.dbn.diagnostics.Diagnostics;
import com.dbn.diagnostics.DiagnosticsManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.util.Messages.showInfoDialog;
import static com.dbn.common.util.Messages.showWarningDialog;
import static com.dbn.nls.NlsResources.txt;

@Slf4j
public class DeveloperModeAction extends ProjectAction {

    public DeveloperModeAction() {
        super(txt("app.diagnostics.action.DeveloperMode"));
    }

    private static void openDiagnosticSettings(Project project) {
        DiagnosticsManager diagnosticsManager = DiagnosticsManager.getInstance(project);
        diagnosticsManager.openDiagnosticsSettings();
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setText(txt("app.diagnostics.action.DeveloperMode"));
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        boolean developerMode = Diagnostics.isDeveloperMode();
        if (developerMode) {
            String remainingTime = Diagnostics.getDeveloperMode().getRemainingTime();
            showWarningDialog(project,
                    txt("msg.diagnostics.title.DeveloperModeActive"),
                    txt("msg.diagnostics.warning.DeveloperModeActive", remainingTime),
                    new String[]{txt("msg.diagnostics.button.DisableNow"), txt("msg.shared.button.Cancel"), txt("msg.shared.button.OpenSettings")}, 0,
                    option -> actionPerformed(project, option, false));
        } else {
            int timeoutMinutes = Diagnostics.getDeveloperMode().getTimeout();
            showInfoDialog(project,
                    txt("msg.diagnostics.title.DeveloperModeInactive"),
                    txt("msg.diagnostics.info.DeveloperModeInactive", timeoutMinutes),
                    new String[]{txt("msg.shared.button.Enable"), txt("msg.shared.button.Cancel"), txt("msg.shared.button.OpenSettings")}, 0,
                    option -> actionPerformed(project, option, true));
        }
    }

    private static void actionPerformed(@NotNull Project project, int option, boolean enabled) {
        if (option == 0) {
            Diagnostics.getDeveloperMode().setEnabled(enabled);
            if (enabled && !Diagnostics.hasEnabledFeatures()) {
                openDiagnosticSettings(project);
            }
        } else if (option == 2) {
            openDiagnosticSettings(project);
        }
    }
}
