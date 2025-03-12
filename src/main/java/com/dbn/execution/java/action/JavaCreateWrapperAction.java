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

package com.dbn.execution.java.action;

import com.dbn.common.thread.Progress;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.connection.SessionId;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.common.execution.JavaExecutionProcessorImpl;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.execution.ExecutionStatus;
import com.dbn.execution.java.JavaExecutionContext;
import com.dbn.execution.java.JavaExecutionInput;
import com.dbn.execution.java.JavaExecutionManager;
import com.dbn.execution.java.wrapper.Wrapper;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.action.AnObjectAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class JavaCreateWrapperAction extends AnObjectAction<DBJavaMethod> {
	private final boolean listElement;
	public JavaCreateWrapperAction(DBJavaMethod method, boolean listElement) {
		super(method);
		this.listElement = listElement;
	}

	@Override
	protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull DBJavaMethod method) {
		method.getParameters();
		JavaExecutionProcessorImpl executionProcessor = new JavaExecutionProcessorImpl(method) {
			@Override
			public String buildExecutionCommand(JavaExecutionInput executionInput, Wrapper wrapper) {
				return "";
			}
		};

		JavaExecutionManager executionManager = JavaExecutionManager.getInstance(project);
		JavaExecutionInput executionInput = executionManager.getExecutionInput(method);
		ConnectionAction.invoke("The Method Execution", false, executionInput,
				action -> Progress.prompt(project, action, true,
						"Loading method details",
						"Loading details of " + method.getQualifiedNameWithType(),
						progress -> {
							ConnectionHandler connection = action.getConnection();
							String methodIdentifier = method.ref().getPath();
							if (connection.isValid()) {
								// load the arguments while in background
								executionInput.initDatabaseElements();
								SessionId targetSessionId = executionInput.getTargetSessionId();
								SchemaId targetSchemaId = executionInput.getTargetSchemaId();

								JavaExecutionContext context = executionInput.initExecution(DBDebuggerType.NONE);
								context.setDebuggerType(DBDebuggerType.NONE);
								context.set(ExecutionStatus.EXECUTING, true);
								try {
									DBNConnection conn = connection.getConnection(targetSessionId, targetSchemaId);
									context.setConnection(conn);

									boolean useFriendlyNames = true;
									context.initWrapper(method, useFriendlyNames);

									executionProcessor.initExecutionWrappers(context);
								} catch (Exception ex) {
									conditionallyLog(ex);
								}
							} else {
								String message =
										"Can not execute method " + methodIdentifier + ".\n" +
												"No connectivity to '" + connection.getName() + "'. " +
												"Please check your connection settings and try again.";
								Messages.showErrorDialog(project, message);
							}
						}
				)
		);
	}

	@Override
	protected void update(
			@NotNull AnActionEvent e,
			@NotNull Presentation presentation,
			@NotNull Project project,
			@Nullable DBJavaMethod target) {
		if (listElement) {
			super.update(e, presentation, project, target);
		} else {
			presentation.setText("Create Wrappers...");
		}
	}
}
