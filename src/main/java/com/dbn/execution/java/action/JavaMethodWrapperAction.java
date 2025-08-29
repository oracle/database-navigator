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

import com.dbn.common.operation.DatabaseOperation;
import com.dbn.connection.ConnectionHandler;
import com.dbn.execution.java.wrapper.JavaExecutionWrapperManager;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.action.AnObjectAction;
import com.dbn.prerequisite.DatabasePrerequisiteManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JavaMethodWrapperAction extends AnObjectAction<DBJavaMethod> {
	public JavaMethodWrapperAction(DBJavaMethod method) {
		super(method);
	}

	@Override
	protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull DBJavaMethod method) {
        DatabasePrerequisiteManager prerequisiteManager = DatabasePrerequisiteManager.getInstance(project);
        ConnectionHandler connection = method.getConnection();

        prerequisiteManager.startOperation(connection, DatabaseOperation.CREATE_JAVA_WRAPPER, () -> {
            JavaExecutionWrapperManager wrapperManager = JavaExecutionWrapperManager.getInstance(project);
            wrapperManager.createExecutionWrappers(method, true, false);
        });
	}

	@Override
	protected void update(
			@NotNull AnActionEvent e,
			@NotNull Presentation presentation,
			@NotNull Project project,
			@Nullable DBJavaMethod target) {
		presentation.setText("Create Execution Wrappers...");
	}
}
