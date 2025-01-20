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

import com.dbn.common.util.Strings;
import com.dbn.database.DatabaseFeature;
import com.dbn.execution.java.JavaExecutionInput;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.lookup.DBObjectRef;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

public class JavaExecutionHistorySimpleTreeModel extends JavaExecutionHistoryTreeModel {
	JavaExecutionHistorySimpleTreeModel(List<JavaExecutionInput> executionInputs, boolean debug) {
		super(executionInputs);
		for (JavaExecutionInput executionInput : executionInputs) {
			if (!executionInput.isObsolete() &&
					!executionInput.isInactive() &&
					(!debug || DatabaseFeature.DEBUGGING.isSupported(executionInput.getConnection()))) {
				RootTreeNode root = getRoot();
				if (root != null) {
					ConnectionTreeNode connectionNode = root.getConnectionNode(executionInput);
					SchemaTreeNode schemaNode = connectionNode.getSchemaNode(executionInput);
					schemaNode.getMethodNode(executionInput);
				}
			}
		}
	}

	@Override
	public List<JavaExecutionInput> getExecutionInputs() {
		List<JavaExecutionInput> executionInputs = new ArrayList<>();
		RootTreeNode root = getRoot();
		if (root != null) {
			for (TreeNode connectionTreeNode : root.getChildren()) {
				ConnectionTreeNode connectionNode = (ConnectionTreeNode) connectionTreeNode;
				for (TreeNode schemaTreeNode : connectionNode.getChildren()) {
					SchemaTreeNode schemaNode = (SchemaTreeNode) schemaTreeNode;
					for (TreeNode node : schemaNode.getChildren()) {
						MethodTreeNode methodNode = (MethodTreeNode) node;
						JavaExecutionInput executionInput =
								getExecutionInput(connectionNode, schemaNode, methodNode);

						if (executionInput != null) {
							executionInputs.add(executionInput);
						}
					}
				}
			}
		}
		return executionInputs;
	}

	private JavaExecutionInput getExecutionInput(
			ConnectionTreeNode connectionNode,
			SchemaTreeNode schemaNode,
			MethodTreeNode methodNode) {
		for (JavaExecutionInput executionInput : executionInputs) {
			DBObjectRef<DBJavaMethod> methodRef = executionInput.getMethodRef();
			if (executionInput.getConnectionId() == connectionNode.getConnectionId() &&
					Strings.equalsIgnoreCase(methodRef.getSchemaName(), schemaNode.getName()) &&
					Strings.equalsIgnoreCase(methodRef.getQualifiedObjectName(), methodNode.getName()) &&
					methodRef.getOverload() == methodNode.getOverload()) {

				return executionInput;
			}
		}
		return null;
	}

	@Override
	public TreePath getTreePath(JavaExecutionInput executionInput) {
		List<JavaExecutionHistoryTreeNode> path = new ArrayList<>();
		RootTreeNode root = getRoot();
		if (root != null) {
			ConnectionTreeNode connectionTreeNode = root.getConnectionNode(executionInput);
			SchemaTreeNode schemaTreeNode = connectionTreeNode.getSchemaNode(executionInput);
			MethodTreeNode methodTreeNode = schemaTreeNode.getMethodNode(executionInput);

			path.add(root);
			path.add(connectionTreeNode);
			path.add(schemaTreeNode);
			path.add(methodTreeNode);
		}

		return new TreePath(path.toArray());
	}
}