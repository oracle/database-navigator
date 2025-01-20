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

import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.tree.DBNTreeModel;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionRef;
import com.dbn.execution.java.JavaExecutionInput;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.Collections;
import java.util.List;

import static com.dbn.connection.ConnectionId.UNKNOWN;

public abstract class JavaExecutionHistoryTreeModel extends DBNTreeModel implements StatefulDisposable {
	protected List<JavaExecutionInput> executionInputs;

	JavaExecutionHistoryTreeModel(List<JavaExecutionInput> executionInputs) {
		super(new RootTreeNode());
		this.executionInputs = executionInputs;
	}

	@Override
	@Nullable
	public RootTreeNode getRoot() {
		return (RootTreeNode) super.getRoot();
	}

	public abstract List<JavaExecutionInput> getExecutionInputs();

	public abstract TreePath getTreePath(JavaExecutionInput executionInput);

	/**********************************************************
	 *                        TreeNodes                       *
	 **********************************************************/
	static class RootTreeNode extends JavaExecutionHistoryTreeNode {
		RootTreeNode() {
			super(null, Type.ROOT, "ROOT");
		}

		ConnectionTreeNode getConnectionNode(JavaExecutionInput executionInput) {
			if (!isLeaf())
				for (TreeNode node : getChildren()) {
					ConnectionTreeNode connectionNode = (ConnectionTreeNode) node;
					if (connectionNode.getConnectionId().equals(executionInput.getMethodRef().getConnectionId())) {
						return connectionNode;
					}
				}

			return new ConnectionTreeNode(this, executionInput);
		}
	}

	protected static class ConnectionTreeNode extends JavaExecutionHistoryTreeNode {
		ConnectionRef connection;

		ConnectionTreeNode(JavaExecutionHistoryTreeNode parent, JavaExecutionInput executionInput) {
			super(parent, Type.CONNECTION, null);
			this.connection = ConnectionRef.of(executionInput.getConnection());
		}

		ConnectionHandler getConnection() {
			return ConnectionRef.get(connection);
		}

		public ConnectionId getConnectionId() {
			ConnectionHandler connection = getConnection();
			return connection == null ? UNKNOWN : connection.getConnectionId();
		}

		@Override
		public String getName() {
			ConnectionHandler connection = getConnection();
			return connection == null ? "[unknown]" : connection.getName();
		}

		@Override
		public Icon getIcon() {
			ConnectionHandler connection = getConnection();
			return connection == null ? Icons.CONNECTION_INVALID : connection.getIcon();
		}

		SchemaTreeNode getSchemaNode(JavaExecutionInput executionInput) {
			if (!isLeaf())
				for (TreeNode node : getChildren()) {
					SchemaTreeNode schemaNode = (SchemaTreeNode) node;
					if (Strings.equalsIgnoreCase(schemaNode.getName(), executionInput.getMethodRef().getSchemaName())) {
						return schemaNode;
					}
				}
			return new SchemaTreeNode(this, executionInput);
		}
	}

	static class SchemaTreeNode extends JavaExecutionHistoryTreeNode {
		SchemaTreeNode(JavaExecutionHistoryTreeNode parent, JavaExecutionInput executionInput) {
			super(parent, Type.SCHEMA, executionInput.getMethodRef().getSchemaName());
		}

		ProgramTreeNode getProgramNode(JavaExecutionInput executionInput) {
			DBObjectRef<DBJavaMethod> methodRef = executionInput.getMethodRef();
			DBObjectRef<?> programRef = methodRef.getParentRef(DBObjectType.PROGRAM);
			String programName = programRef.getObjectName();
			if (!isLeaf())
				for (TreeNode node : getChildren()) {
					if (node instanceof ProgramTreeNode) {
						ProgramTreeNode programNode = (ProgramTreeNode) node;
						if (Strings.equalsIgnoreCase(programNode.getName(), programName)) {
							return programNode;
						}
					}
				}
			return new ProgramTreeNode(this, executionInput);
		}

		MethodTreeNode getMethodNode(JavaExecutionInput executionInput) {
			if (!isLeaf())
				for (TreeNode node : getChildren()) {
					if (node instanceof MethodTreeNode) {
						MethodTreeNode methodNode = (MethodTreeNode) node;
						if (methodNode.getExecutionInput() == executionInput) {
							return methodNode;
						}
					}
				}
			return new MethodTreeNode(this, executionInput);
		}

	}

	static class ProgramTreeNode extends JavaExecutionHistoryTreeNode {
		ProgramTreeNode(JavaExecutionHistoryTreeNode parent, JavaExecutionInput executionInput) {
			super(parent,
					getNodeType(JavaRefUtil.getProgramObjectType(executionInput.getMethodRef())),
					JavaRefUtil.getProgramName(executionInput.getMethodRef()));
		}

		MethodTreeNode getMethodNode(JavaExecutionInput executionInput) {
			DBObjectRef<DBJavaMethod> methodRef = executionInput.getMethodRef();
			String methodName = methodRef.getObjectName();
			short overload = methodRef.getOverload();
			if (!isLeaf())
				for (TreeNode node : getChildren()) {
					MethodTreeNode methodNode = (MethodTreeNode) node;
					if (Strings.equalsIgnoreCase(methodNode.getName(), methodName) && methodNode.getOverload() == overload) {
						return methodNode;
					}
				}
			return new MethodTreeNode(this, executionInput);
		}
	}

	protected static class MethodTreeNode extends JavaExecutionHistoryTreeNode {
		private final JavaExecutionInput executionInput;

		MethodTreeNode(JavaExecutionHistoryTreeNode parent, JavaExecutionInput executionInput) {
			super(parent,
					getNodeType(executionInput.getMethodRef().getObjectType()),
					parent instanceof ProgramTreeNode ?
							executionInput.getMethodRef().getObjectName() :
							executionInput.getMethodRef().getQualifiedObjectName());
			this.executionInput = executionInput;
		}

		short getOverload() {
			return executionInput.getMethodRef().getOverload();
		}

		JavaExecutionInput getExecutionInput() {
			return executionInput;
		}

		@Override
		public boolean isValid() {
			return !executionInput.isObsolete() && !executionInput.isInactive();
		}
	}

	@Override
	public void disposeInner() {
		super.disposeInner();
		executionInputs = Collections.emptyList();
		Dispatch.run(() -> setRoot(new RootTreeNode()));
	}
}