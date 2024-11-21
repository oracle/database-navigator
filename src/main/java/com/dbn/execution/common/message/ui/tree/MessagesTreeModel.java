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

import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.ui.tree.TreeEventType;
import com.dbn.common.ui.tree.Trees;
import com.dbn.common.ui.util.Listeners;
import com.dbn.connection.ConnectionId;
import com.dbn.execution.compiler.CompilerMessage;
import com.dbn.execution.explain.result.ExplainPlanMessage;
import com.dbn.execution.statement.StatementExecutionMessage;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.Enumeration;


public class MessagesTreeModel extends StatefulDisposableBase implements TreeModel, StatefulDisposable {
    private final Listeners<TreeModelListener> listeners = Listeners.create(this);
    private MessagesTreeRootNode rootNode = new MessagesTreeRootNode(this);

    MessagesTreeModel() {

    }

    TreePath addExecutionMessage(StatementExecutionMessage executionMessage) {
        return rootNode.addExecutionMessage(executionMessage);
    }

    TreePath addCompilerMessage(CompilerMessage compilerMessage) {
        return rootNode.addCompilerMessage(compilerMessage);
    }

    TreePath addExplainPlanMessage(ExplainPlanMessage explainPlanMessage) {
        return rootNode.addExplainPlanMessage(explainPlanMessage);
    }

    @Nullable
    public TreePath getTreePath(CompilerMessage compilerMessage) {
        return rootNode.getTreePath(compilerMessage);
    }

    @Nullable
    public TreePath getTreePath(StatementExecutionMessage statementExecutionMessage) {
        return rootNode.getTreePath(statementExecutionMessage);
    }


    public void notifyTreeModelListeners(TreePath treePath, TreeEventType eventType) {
        Trees.notifyTreeModelListeners(this, listeners, treePath, eventType);
    }
    public void notifyTreeModelListeners(TreeNode node, TreeEventType eventType) {
        TreePath treePath = Trees.createTreePath(node);
        notifyTreeModelListeners(treePath, eventType);
    }

    @Override
    public void disposeInner() {
        Disposer.dispose(rootNode);
        listeners.clear();
        rootNode = new MessagesTreeRootNode(this);
    }

   /*********************************************************
    *                       TreeModel                      *
    *********************************************************/
    @Override
    public Object getRoot() {
        return rootNode;
    }

    @Override
    public Object getChild(Object o, int i) {
        TreeNode treeNode = (TreeNode) o;
        return treeNode.getChildAt(i);
    }

    @Override
    public int getChildCount(Object o) {
        TreeNode treeNode = (TreeNode) o;
        return treeNode.getChildCount();
    }

    @Override
    public boolean isLeaf(Object o) {
        TreeNode treeNode = (TreeNode) o;
        return treeNode.isLeaf();
    }

    @Override
    public void valueForPathChanged(TreePath treePath, Object o) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getIndexOfChild(Object o, Object o1) {
        TreeNode treeNode = (TreeNode) o;
        TreeNode childTreeNode = (TreeNode) o1;
        return treeNode.getIndex(childTreeNode);
    }

    @Override
    public void addTreeModelListener(TreeModelListener treeModelListener) {
        listeners.add(treeModelListener);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener treeModelListener) {
        listeners.remove(treeModelListener);
    }

    void removeMessages(ConnectionId connectionId) {
        rootNode.removeMessages(connectionId);
    }

    void resetMessagesStatus() {
        resetMessagesStatus(rootNode);
    }

    private void resetMessagesStatus(TreeNode node) {
        if (node instanceof MessagesTreeLeafNode) {
            MessagesTreeLeafNode messageTreeNode = (MessagesTreeLeafNode) node;
            messageTreeNode.getMessage().setNew(false);
        } else {
            Enumeration<? extends TreeNode> children = node.children();
            while (children.hasMoreElements()) {
                TreeNode treeNode = children.nextElement();
                resetMessagesStatus(treeNode);
            }
        }
    }
}
