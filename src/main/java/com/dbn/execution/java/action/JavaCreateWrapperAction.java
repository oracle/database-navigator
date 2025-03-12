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
import com.dbn.execution.java.wrapper.WrapperStatementExecutor;
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
		String methodSignature = method.getPresentableText();

		ConnectionAction.invoke("creation of execution wrappers", false, method,
				action -> Progress.prompt(project, action, true,
						"Creating Execution Wrappers",
						"Creating execution wrappers for java method \"" + methodSignature + "\"",
						progress -> {
							ConnectionHandler connection = action.getConnection();
							if (connection.isValid()) {
								try {
									WrapperStatementExecutor statementExecutor = new WrapperStatementExecutor();
									statementExecutor.createExecutionWrappers(method, true);
								} catch (Exception ex) {
									Messages.showErrorDialog(project,
											"Error creating execution wrappers for java method \"" + methodSignature + "\"\nCause: " + ex.getMessage());
									conditionallyLog(ex);
								}
							} else {
								String message =
										"Can not create execution wrappers for java method \"" + methodSignature + "\".\n" +
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
			presentation.setText("Create Execution Wrappers...");
		}
	}
}
