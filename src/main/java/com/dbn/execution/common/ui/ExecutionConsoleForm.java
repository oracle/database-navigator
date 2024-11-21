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

package com.dbn.execution.common.ui;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.environment.EnvironmentType;
import com.dbn.common.environment.options.EnvironmentVisibilitySettings;
import com.dbn.common.environment.options.listener.EnvironmentManagerListener;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.icon.Icons;
import com.dbn.common.message.MessageType;
import com.dbn.common.navigation.NavigationInstructions;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.tab.DBNTabbedPane;
import com.dbn.common.ui.tab.DBNTabsSelectionListener;
import com.dbn.common.ui.tab.DBNTabsUpdateListener;
import com.dbn.common.ui.util.ClientProperty;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.execution.ExecutionManager;
import com.dbn.execution.ExecutionResult;
import com.dbn.execution.common.message.ui.ExecutionMessagesForm;
import com.dbn.execution.common.options.ExecutionEngineSettings;
import com.dbn.execution.common.result.ui.ExecutionResultForm;
import com.dbn.execution.compiler.CompilerMessage;
import com.dbn.execution.compiler.CompilerResult;
import com.dbn.execution.explain.result.ExplainPlanMessage;
import com.dbn.execution.explain.result.ExplainPlanResult;
import com.dbn.execution.logging.DatabaseLoggingResult;
import com.dbn.execution.logging.LogOutput;
import com.dbn.execution.logging.LogOutputContext;
import com.dbn.execution.method.result.MethodExecutionResult;
import com.dbn.execution.statement.StatementExecutionInput;
import com.dbn.execution.statement.StatementExecutionMessage;
import com.dbn.execution.statement.options.StatementExecutionSettings;
import com.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dbn.execution.statement.result.StatementExecutionCursorResult;
import com.dbn.execution.statement.result.StatementExecutionResult;
import com.dbn.language.common.DBLanguagePsiFile;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.PsiDocumentTransactionListener;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.dispose.Failsafe.guarded;
import static com.dbn.common.navigation.NavigationInstruction.FOCUS;
import static com.dbn.common.navigation.NavigationInstruction.OPEN;
import static com.dbn.common.navigation.NavigationInstruction.RESET;
import static com.dbn.common.navigation.NavigationInstruction.SCROLL;
import static com.dbn.common.navigation.NavigationInstruction.SELECT;
import static com.dbn.common.ui.tab.DBNTabs.initTabComponent;
import static com.dbn.common.ui.tab.DBNTabs.updateTabColor;
import static com.dbn.common.util.Unsafe.cast;

public class ExecutionConsoleForm extends DBNFormBase {
    private JPanel mainPanel;
    private DBNTabbedPane<DBNForm> resultTabs;
    private ExecutionMessagesForm executionMessagesForm;
    private final Map<ExecutionResult, ExecutionResultForm> executionResultForms = ContainerUtil.createConcurrentWeakKeySoftValueMap();

    private boolean canScrollToSource;

    public ExecutionConsoleForm(Disposable parent, Project project) {
        super(parent, project);
        ProjectEvents.subscribe(project, this, EnvironmentManagerListener.TOPIC, environmentManagerListener());
        ProjectEvents.subscribe(project, this, PsiDocumentTransactionListener.TOPIC, psiDocumentTransactionListener());
    }

    @NotNull
    private EnvironmentManagerListener environmentManagerListener() {
        return new EnvironmentManagerListener() {
            @Override
            public void configurationChanged(Project project) {
                EnvironmentVisibilitySettings visibilitySettings = getEnvironmentSettings(getProject()).getVisibilitySettings();
                for (Component component : getTabbedComponents()) {
                    updateTab(visibilitySettings, component);
                }
            }

            private void updateTab(EnvironmentVisibilitySettings visibilitySettings, Component component) {
                ExecutionResult<?> executionResult = getExecutionResult(component);
                if (executionResult == null) return;

                ConnectionHandler connection = executionResult.getConnection();
                EnvironmentType environmentType = connection.getEnvironmentType();
                if (visibilitySettings.getExecutionResultTabs().value()) {
                    updateTabColor(component, environmentType.getColor());
                } else {
                    updateTabColor(component, null);
                }
            }
        };
    }

    @NotNull
    private PsiDocumentTransactionListener psiDocumentTransactionListener() {
        return new PsiDocumentTransactionListener() {

            @Override
            public void transactionStarted(@NotNull Document document, @NotNull PsiFile file) {

            }

            @Override
            public void transactionCompleted(@NotNull Document document, @NotNull PsiFile file) {
                guarded(() -> refreshResultTabs(file));
            }
        };
    }

    private void refreshResultTabs(@NotNull PsiFile file) {
        List<Component> components = getTabbedComponents();
        DBNTabbedPane<DBNForm> tabs = getResultTabs();

        for (Component component : components) {
            var executionResult = getExecutionResult(component);
            if (executionResult instanceof StatementExecutionResult) {
                StatementExecutionResult statementExecutionResult = (StatementExecutionResult) executionResult;
                StatementExecutionProcessor executionProcessor = statementExecutionResult.getExecutionProcessor();
                if (!isValid(executionProcessor)) continue;
                if (!Objects.equals(file, executionProcessor.getPsiFile())) continue;

                Icon icon = executionProcessor.isDirty() ?
                        Icons.STMT_EXEC_RESULTSET_ORPHAN :
                        Icons.STMT_EXEC_RESULTSET;

                tabs.setTabIcon(component, icon);
            }
        }

        ExecutionMessagesForm messagesPanel = executionMessagesForm;
        if (messagesPanel != null) {
            JComponent messagePanelComponent = messagesPanel.getComponent();
            UserInterface.repaint(messagePanelComponent);
        }
    }


    public DBNTabbedPane<DBNForm> getResultTabs() {
        if (!isValid(resultTabs) && !isDisposed()) {
            resultTabs = new DBNTabbedPane<>(SwingConstants.BOTTOM, this, true);
            mainPanel.removeAll();
            mainPanel.add(resultTabs, BorderLayout.CENTER);
            resultTabs.setFocusable(false);
            //resultTabs.setAdjustBorders(false);
            resultTabs.addTabSelectionListener(tabsSelectionListener);
            resultTabs.addTabUpdateListener(tabsUpdateListener);
            resultTabs.setPopupActions(new ExecutionConsolePopupActionGroup(this));
            resultTabs.setBorder(null);
            Disposer.register(this, resultTabs);
            return resultTabs;
        }
        return Failsafe.nn(resultTabs);
    }

    private int getTabCount() {
        return getResultTabs().getTabCount();
    }

    private final DBNTabsSelectionListener tabsSelectionListener = i -> {
            if (!canScrollToSource) return;
            if (i <= -1) return;

            Component component = getResultTabs().getComponentAt(i);
            ExecutionResult<?> executionResult = getExecutionResult(component);
            if (isNotValid(executionResult)) return;

            if (executionResult instanceof StatementExecutionResult) {
                StatementExecutionResult statementExecutionResult = (StatementExecutionResult) executionResult;
                statementExecutionResult.navigateToEditor(NavigationInstructions.create(FOCUS, SCROLL));
            }
        };

    private final DBNTabsUpdateListener tabsUpdateListener = new DBNTabsUpdateListener() {
        @Override
        public void tabRemoved(int index) {
            if (getTabCount() == 0) {
                getExecutionManager().hideExecutionConsole();
            }
        }
    };

    public void removeAllExceptTab(Component excluded) {
        for (Component component : getTabbedComponents()) {
            if (component != excluded) removeTab(component);
        }
    }

    public synchronized void removeTab(Component component) {
        ExecutionResult<?> executionResult = getExecutionResult(component);
        if (executionResult == null) {
            removeMessagesTab();
        } else {
            removeResultTab(executionResult);
        }
    }

    public void removeAllTabs() {
        for (Component component : getTabbedComponents()) {
            removeTab(component);
        }
    }

    @NotNull
    @Override
    public JComponent getMainComponent() {
        return getResultTabs();
    }

    public void selectResult(StatementExecutionResult executionResult, NavigationInstructions instructions) {
        StatementExecutionMessage executionMessage = executionResult.getExecutionMessage();
        if (executionMessage != null) {
            prepareMessagesTab(instructions);
            ExecutionMessagesForm messagesPane = ensureExecutionMessagesPanel();
            messagesPane.selectMessage(executionMessage, instructions);
        }
    }

    public void addResult(ExplainPlanResult explainPlanResult, NavigationInstructions instructions) {
        if (explainPlanResult.isError()) {
            prepareMessagesTab(instructions.with(RESET));
            ExecutionMessagesForm messagesPane = ensureExecutionMessagesPanel();
            ExplainPlanMessage explainPlanMessage = new ExplainPlanMessage(explainPlanResult, MessageType.ERROR);
            messagesPane.addExplainPlanMessage(explainPlanMessage, instructions);
        } else {
            showResultTab(explainPlanResult);
        }
    }

    public void addResult(StatementExecutionResult executionResult, NavigationInstructions instructions) {
        ExecutionMessagesForm messagesPane = ensureExecutionMessagesPanel();
        TreePath messageTreePath = null;
        CompilerResult compilerResult = executionResult.getCompilerResult();
        //boolean hasCompilerResult = compilerResult != null;
        //boolean selectMessage = !executionResult.getExecutionProcessor().getExecutionInput().isBulkExecution() && !hasCompilerResult;
        //boolean focusMessage = selectMessage && focusOnExecution();
        StatementExecutionMessage executionMessage = executionResult.getExecutionMessage();
        if (executionResult instanceof StatementExecutionCursorResult) {
            if (executionMessage == null) {
                showResultTab(executionResult);
            } else {
                prepareMessagesTab(instructions.with(RESET));
                messageTreePath = messagesPane.addExecutionMessage(executionMessage, instructions);
            }
        } else if (executionMessage != null) {
            prepareMessagesTab(instructions.with(RESET));
            messageTreePath = messagesPane.addExecutionMessage(executionMessage, instructions);
        }

        if (compilerResult != null) {
            addCompilerResult(compilerResult);
        }
        if (messageTreePath != null) {
            messagesPane.expand(messageTreePath);
        }
    }

    private boolean focusOnExecution() {
        Project project = ensureProject();
        ExecutionEngineSettings executionEngineSettings = ExecutionEngineSettings.getInstance(project);
        StatementExecutionSettings statementExecutionSettings = executionEngineSettings.getStatementExecutionSettings();
        return statementExecutionSettings.isFocusResult();
    }

    public void addCompilerResult(@NotNull CompilerResult compilerResult) {
        boolean bulk = compilerResult.getCompilerAction().isBulkCompile();
        boolean error = compilerResult.isError();
        boolean single = compilerResult.isSingleMessage();

        prepareMessagesTab(NavigationInstructions.create(RESET));

        CompilerMessage firstMessage = null;
        ExecutionMessagesForm messagesPanel = ensureExecutionMessagesPanel();

        for (CompilerMessage compilerMessage : compilerResult.getCompilerMessages()) {
            if (firstMessage == null) {
                firstMessage = compilerMessage;
            }
            messagesPanel.addCompilerMessage(compilerMessage,
                    NavigationInstructions.create().
                    with(FOCUS, !bulk && error && single).
                    with(SCROLL, single));
        }

        if (firstMessage != null && firstMessage.isError() && !bulk) {
            messagesPanel.selectMessage(firstMessage, NavigationInstructions.create(SCROLL, SELECT, OPEN));
        }
    }

    public void addResult(MethodExecutionResult executionResult) {
        showResultTab(executionResult);
    }

    public ExecutionResult<?> getSelectedExecutionResult() {
        var tabs = getResultTabs();
        var component = tabs.getSelectedTabComponent();
        return component == null ? null : getExecutionResult(component);
    }

    @Nullable
    private static ExecutionResult<?> getExecutionResult(Component component) {
        Object content = ClientProperty.TAB_CONTENT.get(component);
        if (content instanceof ExecutionResultForm) {
            ExecutionResultForm<?> executionResultForm = (ExecutionResultForm<?>) content;
            return isValid(executionResultForm) ? executionResultForm.getExecutionResult() : null;
        }
        return null;
    }

    /*********************************************************
     *                       Messages                        *
     *********************************************************/
    private ExecutionMessagesForm ensureExecutionMessagesPanel() {
        if (isNotValid(executionMessagesForm)) {
            executionMessagesForm = new ExecutionMessagesForm(this);
        }
        return executionMessagesForm;
    }

    private void prepareMessagesTab(NavigationInstructions instructions) {
        var tabs = getResultTabs();
        ExecutionMessagesForm messagesPanel = ensureExecutionMessagesPanel();
        if (instructions.isReset()) {
            messagesPanel.resetMessagesStatus();
        }
        JComponent component = messagesPanel.getComponent();
        if (tabs.getTabCount() == 0 || tabs.getComponentAt(0) != component) {
            initTabComponent(component, Icons.EXEC_RESULT_MESSAGES, null, messagesPanel);
            tabs.insertTab("Messages", component, 0);
        }

        tabs.selectTab(component, instructions.isFocus());
    }


    public void removeMessagesTab() {
        ExecutionMessagesForm messagesPanel = this.executionMessagesForm;
        if (messagesPanel == null) return;

        var tabs = getResultTabs();
        JComponent component = messagesPanel.getComponent();
        if (tabs.getTabCount() > 0 && tabs.getComponentAt(0) == component) {
            tabs.removeTab(component, true);
        }

        Disposer.dispose(messagesPanel);
        this.executionMessagesForm = null;
    }

    @NotNull
    public ExecutionManager getExecutionManager() {
        Project project = ensureProject();
        return ExecutionManager.getInstance(project);
    }

    private boolean isMessagesTabVisible() {
        var tabs = getResultTabs();
        if (tabs.getTabCount() > 0) {
            JComponent messagesPanelComponent = ensureExecutionMessagesPanel().getComponent();
            Component component = tabs.getComponentAt(0);
            return component == messagesPanelComponent;
        }
        return false;
    }

    /*********************************************************
     *                       Logging                         *
     *********************************************************/
    public void displayLogOutput(LogOutputContext context, LogOutput output) {
        boolean emptyOutput = Strings.isEmptyOrSpaces(output.getText());
        VirtualFile sourceFile = context.getSourceFile();
        ConnectionHandler connection = context.getConnection();
        boolean selectTab = sourceFile != null;
        var tabs = getResultTabs();

        for (Component component : getTabbedComponents()) {
            ExecutionResult<?> executionResult = getExecutionResult(component);
            if (executionResult instanceof DatabaseLoggingResult) {
                DatabaseLoggingResult logOutput = (DatabaseLoggingResult) executionResult;
                if (!logOutput.matches(context)) continue;

                logOutput.write(context, output);
                if (!emptyOutput && !selectTab) {
                    tabs.setTabIcon(component, Icons.EXEC_LOG_OUTPUT_CONSOLE_UNREAD);
                }
                if (selectTab) {
                    tabs.selectTab(component, true);
                }
                return;
            }
        }
        boolean messagesTabVisible = isMessagesTabVisible();

        DatabaseLoggingResult logOutput = new DatabaseLoggingResult(context);
        ExecutionResultForm<?> resultForm = ensureResultForm(logOutput);
        if (isValid(resultForm)) {
            String title = logOutput.getName();
            Icon icon = emptyOutput || selectTab ?
                    Icons.EXEC_LOG_OUTPUT_CONSOLE :
                    Icons.EXEC_LOG_OUTPUT_CONSOLE_UNREAD;
            Color color = null;
            EnvironmentVisibilitySettings visibilitySettings = getEnvironmentSettings(getProject()).getVisibilitySettings();
            if (visibilitySettings.getExecutionResultTabs().value()) {
                color = connection.getEnvironmentType().getColor();
            }

            JComponent component = resultForm.getComponent();
            initTabComponent(component, icon, color, resultForm);

            int index = messagesTabVisible ? 1 : 0;
            tabs.insertTab(title, component, index);

            if (selectTab) {
                tabs.selectTab(component, true);
            }

            logOutput.write(context, output);
        }
    }

    /*********************************************************
     *           Statement / method executions               *
     *********************************************************/
    private void showResultTab(ExecutionResult<?> executionResult) {
        if (executionResult instanceof ExplainPlanResult) {
            addResultTab(executionResult);
        } else {
            ExecutionResult<?> previousExecutionResult = executionResult.getPrevious();

            ExecutionResultForm executionResultForm;
            if (previousExecutionResult == null) {
                executionResultForm = getExecutionResultForm(executionResult);
                if (executionResultForm != null) {
                    selectResultTab(executionResult);
                }
            } else {
                executionResultForm = getExecutionResultForm(previousExecutionResult);
                if (executionResultForm != null) {
                    executionResultForms.remove(previousExecutionResult);
                    executionResultForms.put(executionResult, executionResultForm);
                    executionResultForm.setExecutionResult(executionResult);
                    selectResultTab(executionResult);
                }
            }

            if (executionResultForm == null) {
                addResultTab(executionResult);
            }
        }
    }

    private void addResultTab(ExecutionResult<?> executionResult) {
        ExecutionResultForm<?> resultForm = ensureResultForm(executionResult);
        if (isNotValid(resultForm)) return;

        String title = executionResult.getName();
        Icon icon = executionResult.getIcon();
        Color color = null;
        EnvironmentVisibilitySettings visibilitySettings = getEnvironmentSettings(getProject()).getVisibilitySettings();
        if (visibilitySettings.getExecutionResultTabs().value()){
            color = executionResult.getConnection().getEnvironmentType().getColor();
        }

        JComponent component = resultForm.getComponent();
        initTabComponent(component, icon, color, resultForm);

        var tabs = getResultTabs();
        tabs.addTab(title, component);
        tabs.selectTab(component);
    }

    public void removeResultTab(ExecutionResult<?> executionResult) {
        try {
            canScrollToSource = false;
            var tabs = getResultTabs();
            ExecutionResultForm<?> resultForm = executionResultForms.remove(executionResult);
            if (resultForm == null) return;

            JComponent component = resultForm.getComponent();
            int index = tabs.getTabIndex(component);
            if (index == -1) return;

            DBLanguagePsiFile file = null;
            if (executionResult instanceof StatementExecutionResult) {
                StatementExecutionResult statementExecutionResult = (StatementExecutionResult) executionResult;
                StatementExecutionInput executionInput = statementExecutionResult.getExecutionInput();
                file = executionInput.getExecutionProcessor().getPsiFile();
            }

            tabs.removeTab(component, true);
            Documents.refreshEditorAnnotations(file);
        } finally {
            canScrollToSource = true;
        }
    }

    public <T extends ExecutionResult<?>> void selectResultTab(T executionResult) {
        ExecutionResultForm<?> resultForm = getExecutionResultForm(executionResult);
        if (isNotValid(resultForm)) return;

        JComponent component = resultForm.getComponent();

        DBNTabbedPane<DBNForm> tabs = getResultTabs();
        int index = tabs.getTabIndex(component);
        if (index == -1) return;

        tabs.setSelectedIndex(index);
        tabs.setIconAt(index, executionResult.getIcon());
        tabs.setTitleAt(index, executionResult.getName());
    }

    public void closeExecutionResults(List<ConnectionId> connectionIds) {
        for (Component component : getTabbedComponents()) {
            ExecutionResult<?> executionResult = getExecutionResult(component);
            if (executionResult == null) continue;

            ConnectionId connectionId = executionResult.getConnectionId();
            if (connectionIds.contains(connectionId)) removeTab(component);
        }
    }

    private List<Component> getTabbedComponents() {
        return isValid(resultTabs) ? new ArrayList<>(resultTabs.getTabbedComponents()) : Collections.emptyList();
    }

    @Nullable
    private ExecutionResultForm<?> ensureResultForm(ExecutionResult<?> executionResult) {
        return executionResultForms.computeIfAbsent(executionResult, k -> executionResult.createForm());
    }

    @Nullable
    public <T extends ExecutionResultForm> T getExecutionResultForm(ExecutionResult<?> executionResult) {
        return cast(executionResultForms.get(executionResult));
    }
}
