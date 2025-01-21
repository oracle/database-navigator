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

package com.dbn.execution.java.history.ui;

import com.dbn.common.action.Lookups;
import com.dbn.common.action.ProjectAction;
import com.dbn.common.action.ToggleAction;
import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Actions;
import com.dbn.connection.ConnectionAction;
import com.dbn.database.DatabaseFeature;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.execution.java.JavaExecutionInput;
import com.dbn.execution.java.JavaExecutionManager;
import com.dbn.execution.java.ui.JavaExecutionHistory;
import com.dbn.execution.java.ui.JavaExecutionInputForm;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.options.ConfigId;
import com.dbn.options.ProjectSettingsManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionListener;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Map;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.ui.util.Splitters.makeRegular;

public class JavaExecutionHistoryForm extends DBNFormBase {
	private JPanel mainPanel;
	private JTree executionInputsTree;
	private JPanel actionsPanel;
	private JPanel argumentsPanel;
	private JPanel contentPanel;
	private JSplitPane contentSplitPane;
	private ChangeListener changeListener;
	private final boolean debug;

	private final Map<DBObjectRef<DBJavaMethod>, JavaExecutionInputForm> methodExecutionForms = DisposableContainers.map(this);

	JavaExecutionHistoryForm(JavaExecutionHistoryDialog parent, JavaExecutionInput selectedExecutionInput, boolean debug) {
		super(parent);
		this.debug = debug;
		ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, true,
				new ShowGroupedTreeAction(),
				new DeleteHistoryEntryAction(),
				Actions.SEPARATOR,
				new ProjectSettingsOpenAction());

		actionsPanel.add(actionToolbar.getComponent());
		makeRegular(contentSplitPane);

		JavaExecutionHistory executionHistory = getExecutionHistory();
		if (selectedExecutionInput != null && !selectedExecutionInput.isObsolete() && !selectedExecutionInput.isInactive() && (!debug || DatabaseFeature.DEBUGGING.isSupported(selectedExecutionInput))) {
			showMethodExecutionPanel(selectedExecutionInput);
			setSelectedInput(selectedExecutionInput);
		}
		List<JavaExecutionInput> executionInputs = executionHistory.getExecutionInputs();
		getTree().init(executionInputs, executionHistory.isGroupEntries());
		executionInputsTree.getSelectionModel().addTreeSelectionListener(treeSelectionListener);
	}

	private JavaExecutionHistory getExecutionHistory() {
		return JavaExecutionManager.getInstance(ensureProject()).getExecutionHistory();
	}

	@NotNull public JavaExecutionHistoryDialog getParentDialog() {
		return ensureParentComponent();
	}

	@NotNull
	@Override
	public JPanel getMainComponent() {
		return mainPanel;
	}

	List<JavaExecutionInput> getExecutionInputs() {
		JavaExecutionHistoryTreeModel model = (JavaExecutionHistoryTreeModel) executionInputsTree.getModel();
		return model.getExecutionInputs();
	}

	private void createUIComponents() {
		JavaExecutionHistory executionHistory = getExecutionHistory();
		boolean group = executionHistory.isGroupEntries();
		executionInputsTree = new JavaExecutionHistoryTree(this, group, debug);
	}

	public JavaExecutionHistoryTree getTree() {
		return (JavaExecutionHistoryTree) executionInputsTree;
	}

	private void showMethodExecutionPanel(JavaExecutionInput executionInput) {
		argumentsPanel.removeAll();
		if (executionInput != null && !executionInput.isObsolete() && !executionInput.isInactive()) {
			DBObjectRef<DBJavaMethod> method = executionInput.getMethodRef();
			JavaExecutionInputForm methodExecutionInputForm = methodExecutionForms.computeIfAbsent(method, m -> createMethodExecutionForm(executionInput));
			argumentsPanel.add(methodExecutionInputForm.getComponent(), BorderLayout.CENTER);
		}

		UserInterface.repaint(argumentsPanel);
	}

	@NotNull
	private JavaExecutionInputForm createMethodExecutionForm(JavaExecutionInput executionInput) {
		JavaExecutionInputForm form = new JavaExecutionInputForm(this, executionInput, true, DBDebuggerType.NONE);
		form.addChangeListener(getChangeListener());
		return form;
	}

	private ChangeListener getChangeListener() {
		if (changeListener == null) {
			changeListener = e -> getParentDialog().setSaveButtonEnabled(true);
		}
		return changeListener;
	}

	void updateMethodExecutionInputs() {
		for (JavaExecutionInputForm methodExecutionComponent : methodExecutionForms.values()) {
			methodExecutionComponent.updateExecutionInput();
		}
	}

	void setSelectedInput(JavaExecutionInput selectedExecutionInput) {
		getTree().setSelectedInput(selectedExecutionInput);
	}

	public class DeleteHistoryEntryAction extends ProjectAction {
		@Override
		protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
			getTree().removeSelectedEntries();
		}

		@Override
		protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
			Presentation presentation = e.getPresentation();
			presentation.setText("Delete");
			presentation.setIcon(Icons.ACTION_REMOVE);
			presentation.setEnabled(!getTree().isSelectionEmpty());
			presentation.setVisible(getParentDialog().isEditable());
		}
	}

	public static class ProjectSettingsOpenAction extends ProjectAction {
		@Override
		protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
			ProjectSettingsManager settingsManager = ProjectSettingsManager.getInstance(project);
			settingsManager.openProjectSettings(ConfigId.EXECUTION_ENGINE);
		}

		@Override
		protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
			Presentation presentation = e.getPresentation();
			presentation.setIcon(Icons.ACTION_OPTIONS);
			presentation.setText("Settings");
		}
	}

	public class ShowGroupedTreeAction extends ToggleAction {
		ShowGroupedTreeAction() {
			super("Group by Program", "Show grouped by program", Icons.ACTION_GROUP);
		}

		@Override
		public boolean isSelected(@NotNull AnActionEvent e) {
			return getTree().isGrouped();
		}

		@Override
		public void setSelected(@NotNull AnActionEvent e, boolean state) {
			getTemplatePresentation().setText(state ? "Ungroup" : "Group by Program");
			JavaExecutionHistoryTree historyTree = getTree();
			List<JavaExecutionInput> executionInputs = historyTree.getModel().getExecutionInputs();
			historyTree.init(executionInputs, state);
			Project project = Lookups.getProject(e);
			if (isNotValid(project)) return;


			JavaExecutionManager executionManager = JavaExecutionManager.getInstance(project);
			executionManager.getExecutionHistory().setGroupEntries(state);
		}
	}


	private final TreeSelectionListener treeSelectionListener = e -> {
		JavaExecutionInput executionInput = getTree().getSelectedExecutionInput();
		if (executionInput != null) {
			ConnectionAction.invoke("loading the execution history", true, executionInput, action -> Progress.prompt(getProject(), action, false, "Loading method details", "Loading details of " + executionInput.getMethodRef().getQualifiedNameWithType(), progress -> {
                                                                /*DBMethod method = executionInput.getMethod();
                                if (method != null) {
                                    method.getArguments();
                                }*/

				Dispatch.run(() -> {
					JavaExecutionHistoryDialog dialog = getParentDialog();
					showMethodExecutionPanel(executionInput);
					dialog.setSelectedExecutionInput(executionInput);
					dialog.updateMainButtons(executionInput);

					JavaExecutionHistory executionHistory = getExecutionHistory();
					executionHistory.setSelection(executionInput.getMethodRef());
				});
			}));
		} else {
			JavaExecutionHistoryDialog dialog = getParentDialog();
			dialog.updateMainButtons(null);
		}
	};
}