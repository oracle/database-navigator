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

import com.dbn.common.ui.tree.DBNColoredTreeCellRenderer;
import com.dbn.common.ui.tree.DBNTree;
import com.dbn.execution.java.JavaExecutionInput;
import com.dbn.execution.java.history.ui.JavaExecutionHistoryTreeModel.MethodTreeNode;
import com.dbn.execution.java.history.ui.JavaExecutionHistoryTreeModel.ProgramTreeNode;
import com.intellij.openapi.Disposable;
import com.intellij.util.ui.tree.TreeUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.util.Collections;
import java.util.List;

import static com.dbn.common.util.Commons.nvl;
import static com.dbn.object.lookup.DBJavaNameCache.getCanonicalName;
import static com.intellij.ui.SimpleTextAttributes.GRAY_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES;

@Getter
public class JavaExecutionHistoryTree extends DBNTree implements Disposable {
	private boolean grouped;
	private final boolean debug;

	JavaExecutionHistoryTree(JavaExecutionHistoryForm form, boolean grouped, boolean debug) {
		super(form, createTreeModel(grouped, debug));
		this.grouped = grouped;
		this.debug = debug;
		setCellRenderer(new TreeCellRenderer());
		getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		TreeUtil.expand(this, 4);

		getModel().addTreeModelListener(treeModelListener);
	}

	@NotNull
	public JavaExecutionHistoryDialog getParentDialog() {
		return ((JavaExecutionHistoryForm) this.getParentComponent()).getParentDialog();
	}

	@NotNull
	private static TreeModel createTreeModel(boolean grouped, boolean debug) {
		return grouped ?
				new JavaExecutionHistoryGroupedTreeModel(Collections.emptyList(), debug) :
				new JavaExecutionHistorySimpleTreeModel(Collections.emptyList(), debug);
	}

	@Override
	public JavaExecutionHistoryTreeModel getModel() {
		return (JavaExecutionHistoryTreeModel) super.getModel();
	}

	void init(List<JavaExecutionInput> executionInputs, boolean grouped) {
		JavaExecutionInput selectedExecutionInput = getSelectedExecutionInput();
		JavaExecutionHistoryTreeModel model = grouped ?
				new JavaExecutionHistoryGroupedTreeModel(executionInputs, debug) :
				new JavaExecutionHistorySimpleTreeModel(executionInputs, debug);
		model.addTreeModelListener(treeModelListener);
		setModel(model);
		TreeUtil.expand(this, 4);
		this.grouped = grouped;
		setSelectedInput(selectedExecutionInput);
	}

	void setSelectedInput(JavaExecutionInput executionInput) {
		if (executionInput == null) return;

		JavaExecutionHistoryTreeModel model = getModel();
		getSelectionModel().setSelectionPath(model.getTreePath(executionInput));
	}

	@Nullable
	JavaExecutionInput getSelectedExecutionInput() {
		Object selection = getLastSelectedPathComponent();
		if (selection instanceof MethodTreeNode) {
			MethodTreeNode methodNode = (MethodTreeNode) selection;
			return methodNode.getExecutionInput();
		}
		return null;
	}

	private static class TreeCellRenderer extends DBNColoredTreeCellRenderer {
		@Override
		public void customizeCellRenderer(DBNTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			JavaExecutionHistoryTreeNode node = (JavaExecutionHistoryTreeNode) value;
			setIcon(node.getIcon());
			String name = node.getName();
			if (node instanceof ProgramTreeNode) {
				append(getCanonicalName(name), REGULAR_ATTRIBUTES);
			}
			else if (node instanceof MethodTreeNode) {
				int overloadIndex = name.lastIndexOf("#");
				String methodPath = overloadIndex > 0 ? name.substring(0, overloadIndex) : name;

				int pathIndex = name.lastIndexOf(".");
				if (pathIndex > 0) {
					String className = getCanonicalName(methodPath.substring(0, pathIndex));
					String methodName = methodPath.substring(pathIndex + 1);
					append(className + "." + methodName, REGULAR_ATTRIBUTES);
				} else {
					append(methodPath, REGULAR_ATTRIBUTES);
				}

				String methodOverload = name.substring(overloadIndex);
				append(" " + methodOverload, GRAY_ATTRIBUTES);
			} else {
				append(nvl(name, ""), REGULAR_ATTRIBUTES);
			}
		}
	}

	void removeSelectedEntries() {
		TreePath selectionPath = getSelectionPath();
		if (selectionPath == null) return;

		JavaExecutionHistoryTreeNode treeNode = (JavaExecutionHistoryTreeNode) selectionPath.getLastPathComponent();
		JavaExecutionHistoryTreeNode parentTreeNode = (JavaExecutionHistoryTreeNode) treeNode.getParent();
		while (parentTreeNode != null &&
				parentTreeNode.getChildCount() == 1 &&
				!parentTreeNode.isRoot()) {
			getSelectionModel().setSelectionPath(TreeUtil.getPathFromRoot(parentTreeNode));
			parentTreeNode = (JavaExecutionHistoryTreeNode) parentTreeNode.getParent();
		}
		TreeUtil.removeSelected(this);
	}

	/**********************************************************
	 *                         Listeners                      *
	 **********************************************************/

	private final TreeModelListener treeModelListener = new TreeModelHandler() {
		@Override
		public void treeNodesRemoved(TreeModelEvent e) {
			getParentDialog().setSaveButtonEnabled(true);
		}
	};
}