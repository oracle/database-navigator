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
import com.dbn.common.data.Data;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.icon.Icons;
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
import com.dbn.execution.common.input.CodeBlocks;
import com.dbn.execution.common.input.ExecutionValue;
import com.dbn.execution.common.input.ValueHolder;
import com.dbn.execution.common.result.ui.ExecutionResultFormBase;
import com.dbn.execution.java.JavaExecutionInput;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        List<ExecutionValue> fieldValues = getInputValues();
        List<ExecutionValue> outputValues = executionResult.getFieldValues();

        argumentValuesTree = new ArgumentValuesTree(this, fieldValues, outputValues);
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

    private @NotNull ArrayList<ExecutionValue> getInputValues() {
        return new ArrayList<>(getExecutionResult().getExecutionInput().getInputValues().values());
    }

    public DBJavaMethod getMethod() {
        JavaExecutionResult executionResult = getExecutionResult();
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
        JavaExecutionResult executionResult = getExecutionResult();
        List<ExecutionValue> inputFieldValues = getInputValues();
        List<ExecutionValue> outputFieldValues = executionResult.getFieldValues();

        DBJavaMethod method = executionResult.getMethod();
        ArgumentValuesTreeModel treeModel = new ArgumentValuesTreeModel(method, inputFieldValues, outputFieldValues);
        argumentValuesTree.setModel(treeModel);
        TreeUtil.expand(argumentValuesTree, 2);
    }

    private void updateOutputTabs() {
        outputTabs.removeAllTabs();
        JavaExecutionResult executionResult = getExecutionResult();
        addInputArgumentTabs(executionResult);
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

    private void addInputArgumentTabs(JavaExecutionResult executionResult) {
        JavaExecutionInput executionInput = executionResult.getExecutionInput();
        Map<String, ExecutionValue<String>> inputValues = executionInput.getInputValues();

        DBJavaMethod method = executionResult.getMethod();
        for (String parameterName : inputValues.keySet()) {
            ExecutionValue<String> inputValue = inputValues.get(parameterName);
            DBJavaParameter parameter = method.getParameter(parameterName);
            if (parameter == null) continue;

            String parameterValue = inputValue.getValue();
            if (CodeBlocks.isCodeBlock(parameterValue)) {
                DBNForm argumentForm = new JavaExecutionCodeResultForm(this, inputValue);
                addDetailTab(parameter, argumentForm);
                continue;
            }

            if (parameter.isArray()) {
                List<String> elements = Data.arrayStringToList(parameterValue, String.class);
                ExecutionValue executionValue = new ExecutionValue<>(parameterName, ValueHolder.basic(elements));
                DBNForm argumentForm = new JavaExecutionArrayResultForm(this, executionValue);
                addDetailTab(parameter, argumentForm);

            }
        }
    }

    private void addOutputArgumentTabs(JavaExecutionResult executionResult) {
        List<ExecutionValue> fieldValues = executionResult.getFieldValues();

        DBJavaMethod method = executionResult.getMethod();
        for (ExecutionValue fieldValue : fieldValues) {
            String fieldPath = fieldValue.getPath();
            DBJavaParameter parameter = method.getParameter(fieldPath);
            if (parameter == null) continue;

            if (parameter.isArray()) {
                DBNForm argumentForm = new JavaExecutionArrayResultForm(this, fieldValue);
                addDetailTab(parameter, argumentForm);
                continue;
            }

            if (fieldValue.isCursor()) {
                DBNForm argumentForm = new JavaExecutionCursorResultForm(this, executionResult, parameter);
                addDetailTab(parameter, argumentForm);

            } else if (fieldValue.isLargeObject() || fieldValue.isLargeValue()) {
                DBNForm argumentForm = new JavaExecutionLargeValueResultForm(this, parameter, fieldValue);
                addDetailTab(parameter, argumentForm);
            }
        }
    }

    private void addDetailTab(DBJavaParameter parameter, DBNForm form) {
        boolean select = outputTabs.getTabCount() == 0;
        String title = parameter.getName();
        JComponent component = form.getComponent();
        DBNTabs.initTabComponent(component, parameter.getIcon(), null, form);

        outputTabs.addTab(title, component);
        if (select) outputTabs.setSelectedIndex(0);
    }

    void selectArgumentOutputTab(DBJavaParameter parameter) {
        for (int index = 0; index < outputTabs.getTabCount(); index++) {
            DBNForm content = outputTabs.getContentAt(index);

            if (content instanceof JavaExecutionResultDetailForm detailForm) {
                if (Objects.equals(detailForm.getValuePath(), parameter.getName())) {
                    outputTabs.setSelectedIndex(index);
                    break;
                }
            }
            if (content instanceof JavaExecutionCursorResultForm cursorResultForm) {
                if (Objects.equals(cursorResultForm.getParameter(), parameter)) {
                    outputTabs.setSelectedIndex(index);
                    break;
                }
            } else if (content instanceof JavaExecutionLargeValueResultForm largeValueResultForm) {
                if (Objects.equals(largeValueResultForm.getParameter(), parameter)) {
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
