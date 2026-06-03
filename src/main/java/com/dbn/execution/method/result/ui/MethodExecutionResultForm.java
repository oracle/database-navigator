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

package com.dbn.execution.method.result.ui;

import com.dbn.common.action.DataKeys;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.tab.DBNTabs;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.ClientProperty;
import com.dbn.common.ui.util.TabbedPanes;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SessionId;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.dbn.execution.common.result.ui.ExecutionResultFormBase;
import com.dbn.execution.logging.LogOutput;
import com.dbn.execution.logging.LogOutputContext;
import com.dbn.execution.logging.ui.DatabaseLoggingResultConsole;
import com.dbn.execution.method.ArgumentValue;
import com.dbn.execution.method.result.MethodExecutionResult;
import com.dbn.object.DBArgument;
import com.dbn.object.DBMethod;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import java.awt.Component;
import java.util.List;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.ui.util.Splitters.setSplitPaneProportion;
import static com.dbn.common.util.Commons.nvl;

public class MethodExecutionResultForm extends ExecutionResultFormBase<MethodExecutionResult> {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JPanel statusPanel;
    private JLabel connectionLabel;
    private JLabel durationLabel;
    private JBTabbedPane outputTabs;
    private JTree argumentValuesTree;
    private JPanel argumentValuesPanel;
    private JPanel resultPanel;
    private JBScrollPane argumentValuesScrollPane;
    private JSplitPane resultSplitPanel;


    public MethodExecutionResultForm(@NotNull MethodExecutionResult executionResult) {
        super(executionResult);
        List<ArgumentValue> inputArgumentValues = executionResult.getExecutionInput().getArgumentValues();
        List<ArgumentValue> outputArgumentValues = executionResult.getArgumentValues();
        argumentValuesTree = new ArgumentValuesTree(this, inputArgumentValues, outputArgumentValues);
        argumentValuesScrollPane.setViewportView(argumentValuesTree);


        createActionsPanel();
        updateOutputTabs();

        argumentValuesPanel.setBorder(Borders.lineBorder(JBColor.border(), 0, 1, 1, 0));
        updateStatusBarLabels();
        setSplitPaneProportion(resultSplitPanel, 0.2);
        TreeUtil.expand(argumentValuesTree, 2);
    }

    public DBMethod getMethod() {
        MethodExecutionResult executionResult = getExecutionResult();
        return executionResult.getMethod();
    }

    public void rebuildForm() {
        dispatch(() -> {
            updateArgumentValueTree();
            updateOutputTabs();
            updateStatusBarLabels();
        });
    }

    private void updateArgumentValueTree() {
        MethodExecutionResult executionResult = getExecutionResult();
        List<ArgumentValue> inputArgumentValues = executionResult.getExecutionInput().getArgumentValues();
        List<ArgumentValue> outputArgumentValues = executionResult.getArgumentValues();

        DBMethod method = executionResult.getMethod();
        ArgumentValuesTreeModel treeModel = new ArgumentValuesTreeModel(method, inputArgumentValues, outputArgumentValues);
        argumentValuesTree.setModel(treeModel);
        TreeUtil.expand(argumentValuesTree, 2);
    }

    private void updateOutputTabs() {
        TabbedPanes.removeAllTabs(outputTabs, true);
        MethodExecutionResult executionResult = getExecutionResult();
        addOutputArgumentTabs(executionResult);
        addLoggingConsoleTab(executionResult);
        UserInterface.repaint(outputTabs);
    }

    private void addLoggingConsoleTab(MethodExecutionResult executionResult) {
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

    private void addOutputArgumentTabs(MethodExecutionResult executionResult) {
        List<ArgumentValue> argumentValues = executionResult.getArgumentValues();
        for (ArgumentValue argumentValue : argumentValues) {
            DBArgument argument = argumentValue.getArgument();
            if (argument == null) continue;

            if (argumentValue.isCursor()) {
                DBNForm argumentForm = new MethodExecutionCursorResultForm(this, executionResult, argument);
                addOutputTab(argument, argumentForm);

            } else if (argumentValue.isLargeObject() || argumentValue.isLargeValue()) {
                DBNForm argumentForm = new MethodExecutionLargeValueResultForm(this, argument, argumentValue);
                addOutputTab(argument, argumentForm);
            }
        }
    }

    private void addOutputTab(DBArgument argument, DBNForm form) {
        boolean select = outputTabs.getTabCount() == 0;
        String title = argument.getName();
        JComponent component = form.getComponent();
        DBNTabs.initTabComponent(component, argument.getIcon(), null, form);

        outputTabs.addTab(title, component);
        if (select) outputTabs.setSelectedIndex(0);
    }

    void selectArgumentOutputTab(DBArgument argument) {
        for (int index = 0; index < outputTabs.getTabCount(); index++) {

            Component component = outputTabs.getComponent(index);
            DBNForm content = ClientProperty.FORM.get(component);

            if (content instanceof MethodExecutionCursorResultForm cursorResultForm) {
                if (cursorResultForm.getArgument().equals(argument)) {
                    outputTabs.setSelectedIndex(index);
                    break;
                }
            } else if (content instanceof MethodExecutionLargeValueResultForm largeValueResultForm) {
                if (largeValueResultForm.getArgument().equals(argument)) {
                    outputTabs.setSelectedIndex(index);
                    break;
                }
            }
        }
    }

    private void updateStatusBarLabels() {
        MethodExecutionResult executionResult = getExecutionResult();
        SessionId sessionId = executionResult.getExecutionInput().getTargetSessionId();
        String connectionType =
                sessionId == SessionId.MAIN ? txt("app.execution.label.MainSession") :
                sessionId == SessionId.POOL ? txt("app.execution.label.PoolSession") : txt("app.execution.label.Session");
        ConnectionHandler connection = executionResult.getConnection();
        connectionLabel.setIcon(connection.getIcon());
        connectionLabel.setText(connection.getName() + connectionType);

        durationLabel.setText(txt("app.execution.label.DurationMillis", executionResult.getExecutionDuration()));
    }



    private void createActionsPanel() {
        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, false, "DBNavigator.ActionGroup.MethodExecutionResult");
        setAccessibleName(actionToolbar, txt("app.execution.aria.MethodExecutionResultActions"));
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
        if (DataKeys.METHOD_EXECUTION_RESULT.is(dataId)) return getExecutionResult();
        return null;
    }
}
