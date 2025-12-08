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

import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.dialog.SelectionListDialog;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionAction;
import com.dbn.execution.java.wrapper.JavaExecutionWrapperManager;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.action.AnObjectAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.operation.DatabaseOperation.CREATE_JAVA_WRAPPER;
import static com.dbn.common.util.Messages.showWarningDialog;
import static com.dbn.common.util.Titles.titleCased;
import static com.dbn.nls.NlsResources.txt;

public class JavaClassWrapperAction extends AnObjectAction<DBJavaClass> {
	public JavaClassWrapperAction(DBJavaClass sourceObject) {
		super(sourceObject);
	}

    @Override
    protected void update(
            @NotNull AnActionEvent e,
            @NotNull Presentation presentation,
            @NotNull Project project,
            @Nullable DBJavaClass target) {
        presentation.setText("Create Execution Wrappers...");
    }

	@Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull DBJavaClass javaClass) {
        CREATE_JAVA_WRAPPER.start(javaClass, () -> createExecutionWrappers(e, javaClass));
    }

    private void createExecutionWrappers(@NotNull AnActionEvent e, DBJavaClass javaClass) {
        Project project = javaClass.getProject();
        String listName = "executable elements";
        String title = txt("msg.objects.title.LoadingObjects", titleCased(listName));
        ConnectionAction.invoke(title, true, javaClass,
                action -> Progress.prompt(project, javaClass, true,
                        txt("prc.objects.title.LoadingObjects"),
                        txt("prc.objects.text.LoadingObjects", listName),
                        progress -> showObjectList(e.getDataContext(), action, javaClass)));
    }

	private void showObjectList(DataContext dataContext, ConnectionAction action, DBJavaClass javaClass) {
        if (action.isCancelled()) return;

        List<DBJavaMethod> objects = new ArrayList<>(javaClass.getStaticMethods());
        if (action.isCancelled()) return;

        Dispatch.run(dataContext, true, () -> {
			Project project = javaClass.getProject();
            if (objects.isEmpty()) {
				showWarningDialog(project, "Create Execution Wrappers",
						"Cannot create execution wrappers for java class \"" + javaClass.getCanonicalName() + "\". " +
						"No public static methods found.");
            } else {
				Dialogs.show(() -> createDialog(javaClass, objects), createDialogCallback(javaClass));

            }
        });
    }

	private static @NotNull SelectionListDialog<DBJavaMethod> createDialog(DBJavaClass javaClass, List<DBJavaMethod> javaMethods) {
		Project project = javaClass.getProject();
		return new SelectionListDialog<>(project, "Select method to create wrapper", javaMethods, null, javaClass);
	}

	private static Dialogs.DialogCallback<SelectionListDialog<DBJavaMethod>> createDialogCallback(DBJavaClass javaClass) {
		return (dialog, exitCode) -> {
			if (exitCode != DialogWrapper.OK_EXIT_CODE) return;
			List<DBJavaMethod> methods = dialog.getSelection();
			if (methods == null || methods.isEmpty()) return;

			Project project = javaClass.getProject();
            JavaExecutionWrapperManager wrapperManager = JavaExecutionWrapperManager.getInstance(project);
            wrapperManager.createExecutionWrappers(javaClass, methods, true, false);
        };
	}
}
