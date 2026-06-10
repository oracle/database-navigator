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

package com.dbn.execution.java;

import com.dbn.DatabaseNavigator;
import com.dbn.common.Priority;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.routine.Consumer;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.ConnectionConfigListener;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.DatabaseFeature;
import com.dbn.database.common.execution.JavaExecutionProcessor;
import com.dbn.database.interfaces.DatabaseExecutionInterface;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.execution.ExecutionManager;
import com.dbn.execution.ExecutionStatus;
import com.dbn.execution.java.browser.JavaBrowserSettings;
import com.dbn.execution.java.browser.ui.JavaExecutionBrowserDialog;
import com.dbn.execution.java.history.ui.JavaExecutionHistoryDialog;
import com.dbn.execution.java.ui.JavaExecutionHistory;
import com.dbn.execution.java.ui.JavaExecutionInputDialog;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBSchema;
import com.dbn.object.common.ui.ObjectTreeModel;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import lombok.Getter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.util.Conditional.when;
import static com.dbn.common.util.Messages.showErrorDialog;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

@State(
		name = JavaExecutionManager.COMPONENT_NAME,
		storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
@Getter
public class JavaExecutionManager extends ProjectComponentBase implements PersistentState {
	public static final String COMPONENT_NAME = "DBNavigator.Project.JavaExecutionManager";

	private final JavaBrowserSettings browserSettings = new JavaBrowserSettings();
	private final JavaExecutionHistory executionHistory = new JavaExecutionHistory(getProject());

	private JavaExecutionManager(Project project) {
		super(project, COMPONENT_NAME);
		ProjectEvents.subscribe(project, this, ConnectionConfigListener.TOPIC, connectionConfigListener());
	}

	@NotNull
	private ConnectionConfigListener connectionConfigListener() {
		return new ConnectionConfigListener() {
			@Override
			public void connectionRemoved(ConnectionId connectionId) {
				browserSettings.connectionRemoved(connectionId);
				executionHistory.connectionRemoved(connectionId);
			}
		};
	}

	public static JavaExecutionManager getInstance(@NotNull Project project) {
		return projectService(project, JavaExecutionManager.class);
	}

	public JavaExecutionInput getExecutionInput(DBJavaMethod method) {
		return executionHistory.getExecutionInput(method);
	}

	public String getJavaVersion(ConnectionId connectionId) throws SQLException {
		ConnectionHandler connection = ConnectionHandler.ensure(connectionId);
		return DatabaseInterfaceInvoker.load(Priority.MEDIUM,
				txt("prc.java.title.LoadingJavaVersion"),
				txt("prc.java.text.LoadingDatabaseJavaVersion"),
				getProject(),
				connectionId,
				c -> {
					DatabaseMetadataInterface metadataInterface = connection.getMetadataInterface();
					return metadataInterface.loadJavaVersion(c);
				});
	}

	@NotNull
	public JavaExecutionInput getExecutionInput(DBObjectRef<DBJavaMethod> methodRef) {
		return executionHistory.getExecutionInput(methodRef, true);
	}

	public void startMethodExecution(@NotNull JavaExecutionInput executionInput, @NotNull DBDebuggerType debuggerType) {
		promptExecutionDialog(executionInput, debuggerType, () -> execute(executionInput));
	}

	public void startMethodExecution(@NotNull DBJavaMethod method, @NotNull DBDebuggerType debuggerType) {
        promptExecutionDialog(method, debuggerType, () -> execute(method));
	}

	private void promptExecutionDialog(@NotNull DBJavaMethod method, @NotNull DBDebuggerType debuggerType, Runnable callback) {
		JavaExecutionInput executionInput = getExecutionInput(method);
        executionInput.resetExecutionContext();
		promptExecutionDialog(executionInput, debuggerType, callback);
	}

	public void promptExecutionDialog(JavaExecutionInput executionInput, @NotNull DBDebuggerType debuggerType, Runnable callback) {
		Project project = executionInput.getProject();
		DBObjectRef<DBJavaMethod> methodRef = executionInput.getMethodRef();

		ConnectionAction.invoke(txt("msg.execution.title.MethodExecution"), false, executionInput,
				action -> Progress.prompt(project, action, true,
						txt("prc.execution.title.LoadingMethodDetails"),
						txt("prc.execution.text.LoadingMethodDetails", methodRef.getQualifiedNameWithType()),
						progress -> {
							ConnectionHandler connection = action.getConnection();
							String methodIdentifier = methodRef.getPath();
							if (connection.isValid()) {
								DBJavaMethod method = executionInput.getMethod();
								if (method == null) {
									showErrorDialog(project, txt("msg.execution.message.MethodNotFound", methodIdentifier));
								} else {
									// load the arguments while in background
									executionInput.initDatabaseElements();
									showInputDialog(executionInput, debuggerType, callback);
								}
							} else {
								showErrorDialog(project, txt("msg.execution.message.MethodExecutionConnectivityError", methodIdentifier, connection.getName()));
							}
						}));
	}

	private void showInputDialog(@NotNull JavaExecutionInput executionInput, @NotNull DBDebuggerType debuggerType, @NotNull Runnable executor) {
		Dialogs.show(() -> new JavaExecutionInputDialog(executionInput, debuggerType, executor));
	}


	public void showExecutionHistoryDialog(
			JavaExecutionInput selection,
			boolean editable,
			boolean modal,
			boolean debug,
			Consumer<JavaExecutionInput> callback) {

		Project project = getProject();
		Progress.prompt(project, selection, true,
				txt("prc.execution.title.LoadingDataDictionary"),
				txt("prc.execution.text.LoadingJavaExecutionHistory"),
				progress -> {
					JavaExecutionInput selectedInput = Commons.nvln(selection, executionHistory.getLastSelection());
					if (selectedInput != null) {
						// initialize method arguments while in background
						DBJavaMethod method = selectedInput.getMethod();
						if (isValid(method)) {
							method.getParameters();
						}
					}

					if (!progress.isCanceled()) {
						Dialogs.show(
								() -> new JavaExecutionHistoryDialog(project, selectedInput, editable, modal, debug),
								(dialog, exitCode) -> {
									if (exitCode != DialogWrapper.OK_EXIT_CODE) return;

									JavaExecutionInput newlySelected = dialog.getSelectedExecutionInput();
									if (newlySelected == null) return;
									if (callback == null) return;

									callback.accept(newlySelected);
								});
					}
				});
	}

	public void execute(DBJavaMethod method) {
		JavaExecutionInput executionInput = getExecutionInput(method);
		execute(executionInput);
	}

	public void execute(JavaExecutionInput input) {
		executionHistory.setSelection(input.getMethodRef());
		DBJavaMethod method = input.getMethod();
		JavaExecutionContext context = input.getExecutionContext();
		context.set(ExecutionStatus.EXECUTING, true);

		if (method == null) {
			DBObjectRef<DBJavaMethod> methodRef = input.getMethodRef();
			showErrorDialog(getProject(), txt("msg.execution.message.CannotResolveMethod", methodRef.getQualifiedNameWithType()));
		} else {
			Project project = method.getProject();
			ConnectionHandler connection = Failsafe.nn(method.getConnection());
			DatabaseExecutionInterface executionInterface = connection.getInterfaces().getExecutionInterface();
			JavaExecutionProcessor executionProcessor = executionInterface.createExecutionProcessor(method);

			Progress.prompt(project, method, true,
					txt("prc.execution.title.ExecutingMethod"),
					txt("prc.execution.text.ExecutingMethod", method.getQualifiedNameWithType()),
					progress -> {
						try {
							executionProcessor.execute(input, DBDebuggerType.NONE);
							if (context.isNot(ExecutionStatus.CANCELLED)) {
								ExecutionManager executionManager = ExecutionManager.getInstance(project);
								executionManager.addExecutionResult(input.getExecutionResult());
								context.set(ExecutionStatus.EXECUTING, false);
							}

							context.set(ExecutionStatus.CANCELLED, false);
						} catch (SQLException e) {
							conditionallyLog(e);
							context.set(ExecutionStatus.EXECUTING, false);
							if (context.isNot(ExecutionStatus.CANCELLED)) {
								showErrorDialog(project,
                                        txt("msg.execution.title.MethodExecutionError"),
										txt("msg.execution.message.MethodExecutionError", method.getQualifiedNameWithType(), e.getMessage().trim()),
										new String[]{txt("msg.shared.button.TryAgain"), txt("msg.shared.button.Cancel")}, 0,
										option -> when(option == 0, () ->
												startMethodExecution(input, DBDebuggerType.NONE)));
							}
						}
					});
		}
	}

	public void debugExecute (
			@NotNull JavaExecutionInput input,
			@NotNull DBNConnection conn,
			DBDebuggerType debuggerType) throws SQLException {

		DBJavaMethod method = input.getMethod();
		if (method == null) return;

		ConnectionHandler connection = method.getConnection();
		DatabaseExecutionInterface executionInterface = connection.getInterfaces().getExecutionInterface();
		JavaExecutionProcessor executionProcessor = executionInterface.createDebugExecutionProcessor(method);

		executionProcessor.execute(input, conn, debuggerType);
		JavaExecutionContext context = input.getExecutionContext();
		if (context.isNot(ExecutionStatus.CANCELLED)) {
			ExecutionManager executionManager = ExecutionManager.getInstance(method.getProject());
			executionManager.addExecutionResult(input.getExecutionResult());
		}
		context.set(ExecutionStatus.CANCELLED, false);
	}

	public void promptMethodBrowserDialog(
			@Nullable JavaExecutionInput executionInput, boolean debug,
			@Nullable Consumer<JavaExecutionInput> callback) {

		Progress.prompt(getProject(), executionInput, true,
				txt("prc.execution.title.LoadingDataDictionary"),
				txt("prc.execution.text.LoadingExecutableElements"),
				progress -> {
					Project project = getProject();
					JavaExecutionManager executionManager = JavaExecutionManager.getInstance(project);
					JavaBrowserSettings settings = executionManager.getBrowserSettings();
					DBJavaMethod currentMethod = executionInput == null ? null : executionInput.getMethod();
					if (currentMethod != null) {
						currentMethod.getParameters();
						settings.setSelectedConnection(currentMethod.getConnection());
						settings.setSelectedSchema(currentMethod.getSchema());
						settings.setSelectedMethod(currentMethod);
					}

					DBSchema schema = settings.getSelectedSchema();
					ObjectTreeModel objectTreeModel = !debug || DatabaseFeature.DEBUGGING.isSupported(schema) ?
							new ObjectTreeModel(schema, settings.getVisibleObjectTypes(), settings.getSelectedMethod()) :
							new ObjectTreeModel(null, settings.getVisibleObjectTypes(), null);


					Dialogs.show(() -> new JavaExecutionBrowserDialog(project, objectTreeModel, true), (dialog, exitCode) -> {
						if (exitCode != DialogWrapper.OK_EXIT_CODE) return;

						DBJavaMethod method = dialog.getSelectedMethod();
						JavaExecutionInput methodExecutionInput = executionManager.getExecutionInput(method);
						if (callback != null && methodExecutionInput != null) {
							callback.accept(methodExecutionInput);
						}
					});
				});
	}


	public void setExecutionInputs(List<JavaExecutionInput> executionInputs) {
		executionHistory.setExecutionInputs(executionInputs);
	}

	public void cleanupExecutionHistory(List<ConnectionId> connectionIds) {
		executionHistory.cleanupHistory(connectionIds);
	}

	/****************************************
	 *       PersistentStateComponent       *
	 *****************************************/
	@Nullable
	@Override
	public Element getComponentState() {
		Element element = new Element("state");
		Element browserSettingsElement = newElement(element, "method-browser");
		browserSettings.writeConfiguration(browserSettingsElement);

		executionHistory.writeState(element);
		return element;
	}

	@Override
	public void loadComponentState(@NotNull Element element) {
		Element browserSettingsElement = element.getChild("method-browser");
		if (browserSettingsElement != null) {
			browserSettings.readConfiguration(browserSettingsElement);
		}

		executionHistory.readState(element);
	}


	@Override
	public void disposeInner() {
		Disposer.dispose(executionHistory);
		super.disposeInner();
	}
}
