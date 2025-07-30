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

import com.dbn.common.action.BasicAction;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.dialog.SelectionListDialog;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.execution.java.JavaExecutionInput;
import com.dbn.execution.java.wrapper.JavaExecutionWrapperManager;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.util.Messages.showWarningDialog;
import static com.dbn.common.util.Naming.capitalizeWords;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

public class JavaClassWrapperAction extends BasicAction {
	private final DBObjectRef<?> sourceObject;

	public JavaClassWrapperAction(DBJavaClass sourceObject) {
		super("Create Execution Wrappers...");
		this.sourceObject = DBObjectRef.of(sourceObject);
	}

	@Override
	public void actionPerformed(@NotNull AnActionEvent e) {
		DBObject sourceObject = getSourceObject();
		Project project = sourceObject.getProject();
		String listName = "executable elements";
		String title = txt("msg.objects.title.LoadingObjects", capitalizeWords(listName));
		ConnectionAction.invoke(title, true, sourceObject,
				action -> Progress.prompt(project, sourceObject, true,
						txt("prc.objects.title.LoadingObjects"),
						txt("prc.objects.text.LoadingObjects", listName),
						progress -> showObjectList(e.getDataContext(), action)));
	}

	@NotNull
	public DBObject getSourceObject() {
		return DBObjectRef.ensure(sourceObject);
	}

	private void showObjectList(DataContext dataContext, ConnectionAction action) {
        if (action.isCancelled()) return;

        DBJavaClass javaClass = (DBJavaClass) getSourceObject();

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
			Progress.prompt(project, javaClass, true,
					"Creating execution wrappers",
					"Creating execution wrappers for java class \"" + javaClass.getCanonicalName() + "\"",
					progress -> {
						createExecutionWrappers(javaClass, methods);
					});
		};
	}

	private static void createExecutionWrappers(DBJavaClass javaClass, List<DBJavaMethod> selectedMethods) {
		Project project = javaClass.getProject();
		ConnectionHandler connection = javaClass.getConnection();
		if (connection.isValid()) {
			try {
				for(DBJavaMethod javaMethod : selectedMethods){
					JavaExecutionInput javaExecutionInput = new JavaExecutionInput(project, DBObjectRef.of(javaMethod));
					javaExecutionInput.initDatabaseElements();
				}
				JavaExecutionWrapperManager wrapperManager = JavaExecutionWrapperManager.getInstance(project);
				wrapperManager.createExecutionWrappers(javaClass, selectedMethods, true, false);
			} catch (Exception ex) {
				Messages.showErrorDialog(project,"Error creating execution wrappers for java methods \nCause: " + ex.getMessage());
				conditionallyLog(ex);
			}
		} else {
			String message =
					"Can not create execution wrappers for java methods.\n" +
							"No connectivity to '" + connection.getName() + "'. " +
							"Please check your connection settings and try again.";
			Messages.showErrorDialog(project, message);
		}
	}
}
