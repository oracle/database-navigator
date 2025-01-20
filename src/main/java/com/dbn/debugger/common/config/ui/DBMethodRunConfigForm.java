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

package com.dbn.debugger.common.config.ui;

import com.dbn.common.action.ProjectAction;
import com.dbn.common.action.ProjectPopupAction;
import com.dbn.common.color.Colors;
import com.dbn.common.environment.options.EnvironmentSettings;
import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Actions;
import com.dbn.debugger.ExecutionConfigManager;
import com.dbn.debugger.common.config.DBMethodRunConfig;
import com.dbn.debugger.common.config.DBRunConfigCategory;
import com.dbn.execution.method.MethodExecutionInput;
import com.dbn.execution.method.MethodExecutionManager;
import com.dbn.execution.method.ui.MethodExecutionInputForm;
import com.dbn.object.DBMethod;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;

import static com.dbn.common.dispose.Disposer.replace;

public class DBMethodRunConfigForm extends DBProgramRunConfigForm<DBMethodRunConfig> {
    private JPanel headerPanel;
    private JPanel mainPanel;
    private JPanel methodArgumentsPanel;
    private JPanel selectMethodActionPanel;
    private JPanel hintPanel;
    private MethodExecutionInputForm inputForm;

    public DBMethodRunConfigForm(DBMethodRunConfig configuration) {
        super(configuration.getProject(), configuration.getDebuggerType());
        readConfiguration(configuration);
        if (configuration.getCategory() != DBRunConfigCategory.CUSTOM) {
            selectMethodActionPanel.setVisible(false);
            methodArgumentsPanel.setVisible(false);
            headerPanel.setVisible(false);
            hintPanel.setVisible(true);
            DBNHintForm hintForm = new DBNHintForm(this, ExecutionConfigManager.GENERIC_METHOD_RUNNER_HINT, null, true);
            hintPanel.add(hintForm.getComponent());
        } else {
            ActionToolbar actionToolbar = Actions.createActionToolbar(selectMethodActionPanel, true, new SelectMethodAction());
            selectMethodActionPanel.add(actionToolbar.getComponent(), BorderLayout.WEST);
            hintPanel.setVisible(false);
        }
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public class SelectMethodAction extends ProjectPopupAction {
        @Override
        public AnAction[] getChildren(AnActionEvent e) {
            return new AnAction[]{
                    new MethodHistoryOpenAction(),
                    new MethodBrowserOpenAction()
            };
        }


        @Override
        public void update(@NotNull AnActionEvent e, Project project) {
            Presentation presentation = e.getPresentation();
            presentation.setText("Select Method");
            presentation.setIcon(Icons.DBO_METHOD);
        }
    }

    public class MethodBrowserOpenAction extends ProjectAction {
        @Override
        protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
            MethodExecutionManager executionManager = MethodExecutionManager.getInstance(project);
            executionManager.promptMethodBrowserDialog(getExecutionInput(), true,
                    (executionInput) -> setExecutionInput(executionInput, true));
        }

        @Override
        protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
            e.getPresentation().setText("Method Browser");
        }
    }
    public class MethodHistoryOpenAction extends ProjectAction {

        @Override
        protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
            Presentation presentation = e.getPresentation();
            presentation.setText("Execution History");
            presentation.setIcon(Icons.METHOD_EXECUTION_HISTORY);
        }

        @Override
        protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
            MethodExecutionManager methodExecutionManager = MethodExecutionManager.getInstance(project);
            methodExecutionManager.showExecutionHistoryDialog(getExecutionInput(), false, true, true,
                    (executionInput) -> setExecutionInput(executionInput, true));
        }
    }

    @Nullable
    public MethodExecutionInput getExecutionInput() {
        return inputForm == null ? null : inputForm.getExecutionInput();
    }

    public void setExecutionInput(@Nullable MethodExecutionInput executionInput, boolean touchForm) {
        Progress.modal(getProject(), executionInput, false,
                txt("prc.execution.title.LoadingDataDictionary"),
                txt("prc.execution.text.LoadingMethodInformation"),
                progress -> {
                    // initialize method and arguments
                    initialiseExecutionInput(executionInput, progress);
                    Dispatch.run(() -> initializeInputForm(executionInput, touchForm));
                });
    }

    private void initializeInputForm(@Nullable MethodExecutionInput executionInput, boolean touchForm) {
        checkDisposed();
        methodArgumentsPanel.removeAll();
        inputForm = replace(inputForm, null);

        String headerTitle = "No method selected";
        Icon headerIcon = null;
        Color headerBackground = Colors.getPanelBackground();

        if (executionInput != null) {
            DBObjectRef<DBMethod> methodRef = executionInput.getMethodRef();
            headerTitle = methodRef.getPath();
            headerIcon = methodRef.getObjectType().getIcon();
            DBMethod method = executionInput.getMethod();
            if (method != null) {
                inputForm = new MethodExecutionInputForm(this, executionInput, false, getDebuggerType());
                methodArgumentsPanel.add(inputForm.getComponent(), BorderLayout.CENTER);
                if (touchForm) inputForm.touch();

                headerIcon = method.getOriginalIcon();
                EnvironmentSettings environmentSettings = getEnvironmentSettings(method.getProject());
                if (environmentSettings.getVisibilitySettings().getDialogHeaders().value()) {
                    headerBackground = method.getEnvironmentType().getColor();
                }
            }
        }

        DBNHeaderForm headerForm = new DBNHeaderForm(
                this, headerTitle,
                headerIcon,
                headerBackground
        );
        headerPanel.removeAll();
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

        UserInterface.repaint(mainPanel);
    }

    private void initialiseExecutionInput(@Nullable MethodExecutionInput executionInput, ProgressIndicator progress) {
        checkDisposed(progress);
        if (executionInput == null) return;

        DBMethod method = executionInput.getMethod();
        if (method == null) return;
        checkDisposed(progress);

        method.getArguments();
        checkDisposed(progress);
    }

    @Override
    public void writeConfiguration(DBMethodRunConfig configuration) throws ConfigurationException {
        if (inputForm == null) return;

        inputForm.updateExecutionInput();
        configuration.setExecutionInput(getExecutionInput());
    }

    @Override
    public void readConfiguration(DBMethodRunConfig configuration) {
        setExecutionInput(configuration.getExecutionInput(), false);
    }
}
