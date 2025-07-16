/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.debugger.common.config;

import com.dbn.common.util.Cloneable;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.DatabaseFeature;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.debugger.DatabaseDebuggerManager;
import com.dbn.debugger.common.config.ui.DBJavaRunConfigEditor;
import com.dbn.debugger.jdwp.state.DBJdwpJavaRunProfileState;
import com.dbn.debugger.options.DebuggerTypeOption;
import com.dbn.execution.java.JavaExecutionInput;
import com.dbn.execution.java.JavaExecutionManager;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBMethod;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.options.setting.Settings.newElement;

public class DBJavaRunConfig extends DBRunConfig<JavaExecutionInput> implements Cloneable<DBJavaRunConfig> {
    private Map<DBObjectRef<DBJavaMethod>, JavaExecutionInput> javaSelectionHistory = new HashMap<>();

    public DBJavaRunConfig(Project project, DBJavaRunConfigFactory factory, String name, DBRunConfigCategory category) {
        super(project, factory, name, category);
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new DBJavaRunConfigEditor(this);
    }

    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
        DBDebuggerType debuggerType = getDebuggerType();
//        return debuggerType == DBDebuggerType.JDBC ? new DBJdbcMethodRunProfileState(env) :
        return debuggerType == DBDebuggerType.JDWP ? new DBJdwpJavaRunProfileState(env) : null;
    }

    public Collection<JavaExecutionInput> getJavaSelectionHistory() {
        return javaSelectionHistory.values();
    }

    @Override
    public void setExecutionInput(JavaExecutionInput executionInput) {
        JavaExecutionInput input = getExecutionInput();
        if (input != null && !input.equals(executionInput)) {
            javaSelectionHistory.put(input.getMethodRef(), input);
        }
        super.setExecutionInput(executionInput);
    }

    @Override
    public boolean canRun() {
        if (!super.canRun()) return false;
        if (getJavaMethod() == null) return false;

        DebuggerTypeOption debuggerTypeOption = getJavaMethod().getConnection().getSettings().getDebuggerSettings().getDebuggerType().getSelectedOption();
        if (debuggerTypeOption == DebuggerTypeOption.JDWP) {
            return DBDebuggerType.JDWP.isSupported();
        }
        return true;
    }

    @Override
    public JavaExecutionInput getExecutionInput() {
        return super.getExecutionInput();
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        if (getCategory() != DBRunConfigCategory.CUSTOM) return;

        JavaExecutionInput executionInput = getExecutionInput();
        if (executionInput == null) {
            throw new RuntimeConfigurationError("No or invalid method selected. The database connection is down, obsolete or method has been dropped.");
        }

        if (executionInput.isObsolete()) {
            throw new RuntimeConfigurationError(
                    "Method " + executionInput.getMethodRef().getQualifiedName() + " could not be resolved. " +
                            "The database connection is down or method has been dropped.");
        }

        DBJavaMethod method = getJavaMethod();
        if (method == null) return;

        ConnectionHandler connection = method.getConnection();
        if (!DatabaseFeature.DEBUGGING.isSupported(connection)){
            throw new RuntimeConfigurationError(
                    "Debugging is not supported for " + connection.getDatabaseType().getName() +" databases.");
        }

        DebuggerTypeOption debuggerTypeOption = connection.getSettings().getDebuggerSettings().getDebuggerType().getSelectedOption();
        if (debuggerTypeOption == DebuggerTypeOption.JDWP) {
            DatabaseDebuggerManager.checkJdwpConfiguration();
        }
    }

    @Nullable
    @Override
    public DBJavaMethod getDatabaseContext() {
        return getJavaMethod();
    }

    @Nullable
    public DBJavaMethod getJavaMethod() {
        JavaExecutionInput executionInput = getExecutionInput();
        return executionInput == null ? null : executionInput.getMethod();
    }

    @Override
    public List<DBObjectRef<DBMethod>> getMethodRefs() {
        return Collections.emptyList();
    }

    @Override
    public void writeExternal(@NotNull Element element) throws WriteExternalException {
        super.writeExternal(element);
        JavaExecutionInput executionInput = getExecutionInput();
        if (executionInput != null && getCategory() == DBRunConfigCategory.CUSTOM) {
            Element methodIdentifierElement = newElement(element, "java-identifier");
            executionInput.getMethodRef().writeState(methodIdentifierElement);

            Element methodIdentifierHistoryElement = newElement(element, "java-identifier-history");
            for (JavaExecutionInput histExecutionInput : javaSelectionHistory.values()) {
                methodIdentifierElement = newElement(methodIdentifierHistoryElement, "java-identifier");
                histExecutionInput.getMethodRef().writeState(methodIdentifierElement);
            }
        }
    }

    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);
        JavaExecutionManager executionManager = JavaExecutionManager.getInstance(getProject());
        if (getCategory() == DBRunConfigCategory.CUSTOM) {
            Element methodIdentifierElement = element.getChild("java-identifier");
            if (methodIdentifierElement != null) {
                DBObjectRef<DBJavaMethod> methodRef = new DBObjectRef<>();
                methodRef.readState(methodIdentifierElement);

                JavaExecutionInput executionInput = executionManager.getExecutionInput(methodRef);
                setExecutionInput(executionInput);
            }

            Element methodIdentifierHistoryElement = element.getChild("java-identifier-history");
            if (methodIdentifierHistoryElement != null) {
                for (Element child : methodIdentifierHistoryElement.getChildren()) {
                    DBObjectRef<DBJavaMethod> methodRef = new DBObjectRef<>();
                    methodRef.readState(child);

                    JavaExecutionInput executionInput = executionManager.getExecutionInput(methodRef);
                    javaSelectionHistory.put(methodRef, executionInput);
                }
            }
        }
    }

    @Override
    public DBJavaRunConfig clone() {
        DBJavaRunConfig runConfiguration = (DBJavaRunConfig) super.clone();
        JavaExecutionInput executionInput = getExecutionInput();
        runConfiguration.setExecutionInput(executionInput == null ? null : executionInput.clone());
        runConfiguration.javaSelectionHistory = new HashMap<>(javaSelectionHistory);

        return runConfiguration;
    }

    @Override
    public String suggestedName() {
        if (getCategory() == DBRunConfigCategory.CUSTOM) {
            JavaExecutionInput executionInput = getExecutionInput();
            if (executionInput != null) {
                setGeneratedName(true);
                String runnerName = executionInput.getMethodRef().getObjectName();
                if (getDebuggerType() == DBDebuggerType.JDWP) {
                    runnerName = runnerName + " (JDWP)";
                }
                return runnerName;
            }
        } else {
            String defaultRunnerName = getType().getDefaultRunnerName();
            if (getDebuggerType() == DBDebuggerType.JDWP) {
                defaultRunnerName = defaultRunnerName + " (JDWP)";
            }
            return defaultRunnerName;
        }
        return null;
    }

    @Override
    public Icon getIcon() {
        Icon defaultIcon = super.getIcon();
        if (getCategory() != DBRunConfigCategory.CUSTOM) return defaultIcon;

        JavaExecutionInput executionInput = getExecutionInput();
        if (executionInput == null) return defaultIcon;

        DBJavaMethod method = executionInput.getMethod();
        if (method == null) return defaultIcon;

        return method.getIcon();
    }
}
