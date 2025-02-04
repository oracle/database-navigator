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

import com.dbn.connection.ConnectionHandler;
import com.dbn.execution.java.JavaExecutionInput;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.util.Strings.equalsIgnoreCase;
import static com.dbn.database.DatabaseFeature.DEBUGGING;
import static java.util.Collections.emptyList;

public class JavaExecutionHistoryGroupedTreeModel extends JavaExecutionHistoryTreeModel {

	JavaExecutionHistoryGroupedTreeModel(List<JavaExecutionInput> executionInputs, boolean debug) {
		super(executionInputs);
		for (JavaExecutionInput executionInput : executionInputs) {
            if (executionInput.isObsolete()) continue;
            if (executionInput.isInactive()) continue;
            if (debug && !DEBUGGING.isSupported(executionInput.getConnection())) continue;

            RootTreeNode root = getRoot();
            if (root == null) continue;

            ConnectionTreeNode connectionNode = root.getConnectionNode(executionInput);
            SchemaTreeNode schemaNode = connectionNode.getSchemaNode(executionInput);

            DBObjectRef<DBJavaMethod> methodRef = executionInput.getMethodRef();
            DBObjectRef<?> parentRef = methodRef.getParentRef(DBObjectType.JAVA_CLASS);
            if (parentRef != null) {
                ProgramTreeNode programNode = schemaNode.getProgramNode(executionInput);
                programNode.getMethodNode(executionInput);
            } else {
                schemaNode.getMethodNode(executionInput);
            }
        }
	}

	@Override
	public TreePath getTreePath(JavaExecutionInput executionInput) {
		List<JavaExecutionHistoryTreeNode> path = new ArrayList<>();
		RootTreeNode root = getRoot();
		if (root != null) {
			ConnectionTreeNode connectionTreeNode = root.getConnectionNode(executionInput);
			SchemaTreeNode schemaTreeNode = connectionTreeNode.getSchemaNode(executionInput);

			path.add(root);
			path.add(connectionTreeNode);
			path.add(schemaTreeNode);
			if (executionInput.getMethodRef().getParentObject(DBObjectType.JAVA_CLASS) != null) {
				ProgramTreeNode programTreeNode = schemaTreeNode.getProgramNode(executionInput);
				path.add(programTreeNode);
				MethodTreeNode methodTreeNode = programTreeNode.getMethodNode(executionInput);
				path.add(methodTreeNode);
			} else {
				MethodTreeNode methodTreeNode = schemaTreeNode.getMethodNode(executionInput);
				path.add(methodTreeNode);
			}
		}

		return new TreePath(path.toArray());
	}

	@Override
	public List<JavaExecutionInput> getExecutionInputs() {
		RootTreeNode root = getRoot();
        if (root == null) return emptyList();

        List<TreeNode> children = root.getChildren();
        if (children == null || children.isEmpty()) return emptyList();

        List<JavaExecutionInput> executionInputs = new ArrayList<>();
        for (TreeNode connectionTreeNode : children) {
            ConnectionTreeNode connectionNode = (ConnectionTreeNode) connectionTreeNode;
            for (TreeNode schemaTreeNode : connectionNode.getChildren()) {
                SchemaTreeNode schemaNode = (SchemaTreeNode) schemaTreeNode;
                for (TreeNode node : schemaNode.getChildren()) {
                    if (node instanceof ProgramTreeNode) {
                        ProgramTreeNode programNode = (ProgramTreeNode) node;
                        for (TreeNode methodTreeNode : programNode.getChildren()) {
                            MethodTreeNode methodNode = (MethodTreeNode) methodTreeNode;
                            JavaExecutionInput executionInput =
                                    getExecutionInput(connectionNode, schemaNode, programNode, methodNode);

                            if (executionInput != null) {
                                executionInputs.add(executionInput);
                            }
                        }

                    } else {
                        MethodTreeNode methodNode = (MethodTreeNode) node;
                        JavaExecutionInput executionInput =
                                getExecutionInput(connectionNode, schemaNode, null, methodNode);

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
			ProgramTreeNode programNode,
			MethodTreeNode methodNode) {
		for (JavaExecutionInput executionInput : executionInputs) {
			DBObjectRef<DBJavaMethod> methodRef = executionInput.getMethodRef();
			ConnectionHandler connection = executionInput.getConnection();
			if (connection != null && connection.getConnectionId().equals(connectionNode.getConnectionId()) &&
					equalsIgnoreCase(methodRef.getSchemaName(), schemaNode.getName()) &&
					equalsIgnoreCase(methodRef.getObjectName(), methodNode.getName()) &&
					methodRef.getOverload() == methodNode.getOverload()) {

				DBObjectRef<?> programRef = methodRef.getParentRef(DBObjectType.JAVA_CLASS);
				if (programNode == null && programRef == null) {
					return executionInput;
				} else if (programNode != null && programRef != null) {
					String programName = programNode.getName();
					String inputProgramName = programRef.getObjectName();
					if (equalsIgnoreCase(programName, inputProgramName)) {
						return executionInput;
					}
				}
			}
		}
		return null;
	}
}