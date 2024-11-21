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

package com.dbn.execution.common.message.ui.tree;

import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.tree.TreeEventType;
import com.dbn.connection.ConnectionId;
import com.dbn.execution.common.message.ui.tree.node.CompilerMessagesNode;
import com.dbn.execution.common.message.ui.tree.node.ExplainPlanMessagesNode;
import com.dbn.execution.common.message.ui.tree.node.StatementExecutionMessagesNode;
import com.dbn.execution.compiler.CompilerMessage;
import com.dbn.execution.explain.result.ExplainPlanMessage;
import com.dbn.execution.statement.StatementExecutionMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.List;

public class MessagesTreeRootNode extends MessagesTreeBundleNode<MessagesTreeNode, MessagesTreeBundleNode> {
    private final WeakRef<MessagesTreeModel> treeModel;

    MessagesTreeRootNode(MessagesTreeModel treeModel) {
        super(null);
        this.treeModel = WeakRef.of(treeModel);
    }

    TreePath addExecutionMessage(StatementExecutionMessage executionMessage) {
        StatementExecutionMessagesNode execMessagesNode = null;
        for (TreeNode treeNode : getChildren()) {
            if (treeNode instanceof StatementExecutionMessagesNode) {
                execMessagesNode = (StatementExecutionMessagesNode) treeNode;
                break;
            }
        }
        if (execMessagesNode == null) {
            execMessagesNode = new StatementExecutionMessagesNode(this);
            addChild(execMessagesNode);
            getTreeModel().notifyTreeModelListeners(this, TreeEventType.STRUCTURE_CHANGED);
        }

        return execMessagesNode.addExecutionMessage(executionMessage);
    }

    TreePath addExplainPlanMessage(ExplainPlanMessage explainPlanMessage) {
        ExplainPlanMessagesNode explainPlanMessagesNode = null;
        for (TreeNode treeNode : getChildren()) {
            if (treeNode instanceof ExplainPlanMessagesNode) {
                explainPlanMessagesNode = (ExplainPlanMessagesNode) treeNode;
                break;
            }
        }
        if (explainPlanMessagesNode == null) {
            explainPlanMessagesNode = new ExplainPlanMessagesNode(this);
            addChild(explainPlanMessagesNode);
            getTreeModel().notifyTreeModelListeners(this, TreeEventType.STRUCTURE_CHANGED);
        }

        return explainPlanMessagesNode.addExplainPlanMessage(explainPlanMessage);
    }

    TreePath addCompilerMessage(CompilerMessage compilerMessage) {
        CompilerMessagesNode compilerMessagesNode = getCompilerMessagesNode();
        if (compilerMessagesNode == null) {
            compilerMessagesNode = new CompilerMessagesNode(this);
            addChild(compilerMessagesNode);
            getTreeModel().notifyTreeModelListeners(this, TreeEventType.STRUCTURE_CHANGED);
        }
        return compilerMessagesNode.addCompilerMessage(compilerMessage);
    }

    @Nullable
    private CompilerMessagesNode getCompilerMessagesNode() {
        for (TreeNode node : getChildren()) {
            if (node instanceof CompilerMessagesNode) {
                return (CompilerMessagesNode) node;
            }
        }
        return null;
    }

    @Nullable
    private StatementExecutionMessagesNode getStatementExecutionMessagesNode() {
        for (TreeNode node : getChildren()) {
            if (node instanceof StatementExecutionMessagesNode) {
                return (StatementExecutionMessagesNode) node;
            }
        }
        return null;
    }

    @Nullable
    public TreePath getTreePath(CompilerMessage compilerMessage) {
        CompilerMessagesNode node = getCompilerMessagesNode();
        if (node != null) {
            return node.getTreePath(compilerMessage);
        }
        return null;
    }

    @Nullable
    public TreePath getTreePath(StatementExecutionMessage executionMessage) {
        StatementExecutionMessagesNode node = getStatementExecutionMessagesNode();
        if (node != null) {
            return node.getTreePath(executionMessage);
        }
        return null;
    }

    @Override
    public MessagesTreeModel getTreeModel() {
        return treeModel.ensure();
    }

    @Override
    public void removeMessages(@NotNull ConnectionId connectionId) {
        super.removeMessages(connectionId);
        List<MessagesTreeBundleNode> children = getChildren();
        boolean childrenRemoved = children.removeIf(c -> c.isLeaf());
        if (childrenRemoved) {
            getTreeModel().notifyTreeModelListeners(this, TreeEventType.STRUCTURE_CHANGED);
        }
    }
}
