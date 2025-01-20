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
 *
 */

package com.dbn.execution.common.message.ui.tree.node;

import com.dbn.common.ui.tree.TreeEventType;
import com.dbn.execution.common.message.ui.tree.MessagesTreeBundleNode;
import com.dbn.execution.common.message.ui.tree.MessagesTreeRootNode;
import com.dbn.execution.java.JavaExecutionMessage;
import com.dbn.execution.method.MethodExecutionMessage;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreePath;

public class JavaExecutionMessagesNode  extends MessagesTreeBundleNode<MessagesTreeRootNode, JavaExecutionMessagesObjectNode> {
	public JavaExecutionMessagesNode(MessagesTreeRootNode parent) {
		super(parent);
	}

	private JavaExecutionMessagesObjectNode getChildTreeNode(VirtualFile virtualFile) {
		for (JavaExecutionMessagesObjectNode messagesTreeNode : getChildren()) {
			if (messagesTreeNode.getFile().equals(virtualFile)) {
				return messagesTreeNode;
			}
		}
		return null;
	}

	public TreePath addExecutionMessage(JavaExecutionMessage executionMessage) {
		JavaExecutionMessagesObjectNode objectNode = getChildTreeNode(executionMessage.getContentFile());
		if (objectNode == null) {
			objectNode = new JavaExecutionMessagesObjectNode(this, executionMessage.getDatabaseFile());
			addChild(objectNode);
			getTreeModel().notifyTreeModelListeners(this, TreeEventType.STRUCTURE_CHANGED);
		}
		return objectNode.addCompilerMessage(executionMessage);
	}

	@Nullable
	public TreePath getTreePath(JavaExecutionMessage executionMessage) {
		DBEditableObjectVirtualFile databaseFile = executionMessage.getDatabaseFile();
		JavaExecutionMessagesObjectNode objectNode = getChildTreeNode(databaseFile);
		return objectNode == null ? null : objectNode.getTreePath(executionMessage);
	}
}