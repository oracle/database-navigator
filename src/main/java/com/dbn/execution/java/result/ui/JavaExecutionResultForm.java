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

package com.dbn.execution.java.result.ui;

import com.dbn.common.action.DataKeys;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.tab.DBNTabbedPane;
import com.dbn.common.ui.tab.DBNTabs;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SessionId;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.dbn.execution.common.result.ui.ExecutionResultFormBase;
import com.dbn.execution.java.ArgumentValue;
import com.dbn.execution.java.result.JavaExecutionResult;
import com.dbn.execution.logging.LogOutput;
import com.dbn.execution.logging.LogOutputContext;
import com.dbn.execution.logging.ui.DatabaseLoggingResultConsole;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBJavaParameter;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import java.awt.BorderLayout;
import java.util.List;

import static com.dbn.common.util.Commons.nvl;

public class JavaExecutionResultForm extends ExecutionResultFormBase<JavaExecutionResult> {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JPanel statusPanel;
    private JLabel connectionLabel;
    private JLabel durationLabel;
    private JPanel outputCursorsPanel;
    private JTree argumentValuesTree;
    private JPanel argumentValuesPanel;
    private JPanel executionResultPanel;
    private JBScrollPane argumentValuesScrollPane;

    private final DBNTabbedPane<DBNForm> outputTabs;


    public JavaExecutionResultForm(@NotNull JavaExecutionResult executionResult) {
        super(executionResult);
        List<ArgumentValue> inputArgumentValues = executionResult.getExecutionInput().getArgumentValues();
        argumentValuesTree = new ArgumentValuesTree(this, inputArgumentValues);
        argumentValuesScrollPane.setViewportView(argumentValuesTree);


        outputTabs = new DBNTabbedPane<>(this);
        createActionsPanel();
        updateOutputTabs();

        outputCursorsPanel.add(outputTabs, BorderLayout.CENTER);

        argumentValuesPanel.setBorder(Borders.lineBorder(JBColor.border(), 0, 1, 1, 0));
        updateStatusBarLabels();
        executionResultPanel.setSize(800, -1);
        TreeUtil.expand(argumentValuesTree, 2);
    }

    public DBJavaMethod getMethod() {
        JavaExecutionResult executionResult = getExecutionResult();
        return executionResult.getMethod();
    }

    public void rebuildForm() {
        Dispatch.run(() -> {
            updateArgumentValueTree();
            updateOutputTabs();
            updateStatusBarLabels();
        });
    }

    private void updateArgumentValueTree() {
        JavaExecutionResult executionResult = getExecutionResult();
        List<ArgumentValue> inputArgumentValues = executionResult.getExecutionInput().getArgumentValues();

        DBJavaMethod method = executionResult.getMethod();
        ArgumentValuesTreeModel treeModel = new ArgumentValuesTreeModel(method, inputArgumentValues);
        argumentValuesTree.setModel(treeModel);
        TreeUtil.expand(argumentValuesTree, 2);
    }

    private void updateOutputTabs() {
        outputTabs.removeAllTabs();
        JavaExecutionResult executionResult = getExecutionResult();
        addOutputArgumentTabs(executionResult);
        addLoggingConsoleTab(executionResult);
        UserInterface.repaint(outputTabs);
    }

    private void addLoggingConsoleTab(JavaExecutionResult executionResult) {
        ConnectionHandler connection = executionResult.getConnection();
        DatabaseCompatibilityInterface compatibility = connection.getCompatibilityInterface();
        String logConsoleName = nvl(compatibility.getDatabaseLogName(), "Output");

        DatabaseLoggingResultConsole console = new DatabaseLoggingResultConsole(connection, logConsoleName, true);
        console.setBorder(Borders.lineBorder(JBColor.border(), 0, 0, 1, 0));

        LogOutputContext context = new LogOutputContext(connection);
        console.writeToConsole(context,
                LogOutput.createSysOutput(context,
                        executionResult.getExecutionContext().getExecutionTimestamp(),
                        " - Method execution started", true));

        String logOutput = executionResult.getLogOutput();
        if (Strings.isNotEmptyOrSpaces(logOutput)) {
            console.writeToConsole(context, LogOutput.createStdOutput(logOutput));
        }
        console.writeToConsole(context, LogOutput.createSysOutput(context, " - Method execution finished\n\n", false));
        Disposer.register(this, console);

        outputTabs.addTab(console.getTitle(), Icons.EXEC_LOG_OUTPUT_CONSOLE, console.getComponent());
    }

    private void addOutputArgumentTabs(JavaExecutionResult executionResult) {
        List<ArgumentValue> argumentValues = executionResult.getArgumentValues();
        for (ArgumentValue argumentValue : argumentValues) {
            DBJavaParameter argument = argumentValue.getArgument();
            if (argument == null) continue;

            if (argumentValue.isCursor()) {
                DBNForm argumentForm = new JavaExecutionCursorResultForm(this, executionResult, argument);
                addOutputTab(argument, argumentForm);

            } else if (argumentValue.isLargeObject() || argumentValue.isLargeValue()) {
                DBNForm argumentForm = new JavaExecutionLargeValueResultForm(this, argument, argumentValue);
                addOutputTab(argument, argumentForm);
            }
        }
    }

    private void addOutputTab(DBJavaParameter argument, DBNForm form) {
        boolean select = outputTabs.getTabCount() == 0;
        String title = argument.getName();
        JComponent component = form.getComponent();
        DBNTabs.initTabComponent(component, argument.getIcon(), null, form);

        outputTabs.addTab(title, component);
        if (select) outputTabs.setSelectedIndex(0);
    }

    void selectArgumentOutputTab(DBJavaParameter argument) {
        for (int index = 0; index < outputTabs.getTabCount(); index++) {
            DBNForm content = outputTabs.getContentAt(index);

            if (content instanceof JavaExecutionCursorResultForm) {
                JavaExecutionCursorResultForm cursorResultForm = (JavaExecutionCursorResultForm) content;
                if (cursorResultForm.getArgument().equals(argument)) {
                    outputTabs.setSelectedIndex(index);
                    break;
                }
            } else if (content instanceof JavaExecutionLargeValueResultForm) {
                JavaExecutionLargeValueResultForm largeValueResultForm = (JavaExecutionLargeValueResultForm) content;
                if (largeValueResultForm.getArgument().equals(argument)) {
                    outputTabs.setSelectedIndex(index);
                    break;
                }
            }
        }
    }

    private void updateStatusBarLabels() {
        JavaExecutionResult executionResult = getExecutionResult();
        SessionId sessionId = executionResult.getExecutionInput().getTargetSessionId();
        String connectionType =
                sessionId == SessionId.MAIN ? " (main)" :
                sessionId == SessionId.POOL ? " (pool)" : " (session)";
        ConnectionHandler connection = executionResult.getConnection();
        connectionLabel.setIcon(connection.getIcon());
        connectionLabel.setText(connection.getName() + connectionType);

        durationLabel.setText(": " + executionResult.getExecutionDuration() + " ms");
    }



    private void createActionsPanel() {
        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, false, "DBNavigator.ActionGroup.JavaExecutionResult");
        actionsPanel.add(actionToolbar.getComponent());
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    /********************************************************
     *                    Data Provider                     *
     ********************************************************/
    @Override
    public @Nullable Object getData(@NotNull String dataId) {
        if (DataKeys.JAVA_EXECUTION_RESULT.is(dataId)) return getExecutionResult();
        return null;
    }
}
