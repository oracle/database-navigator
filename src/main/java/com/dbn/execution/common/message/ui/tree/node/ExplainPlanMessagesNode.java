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

package com.dbn.execution.common.message.ui.tree.node;

import com.dbn.common.ui.tree.TreeEventType;
import com.dbn.execution.common.message.ui.tree.MessagesTreeBundleNode;
import com.dbn.execution.common.message.ui.tree.MessagesTreeRootNode;
import com.dbn.execution.explain.result.ExplainPlanMessage;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreePath;

public class ExplainPlanMessagesNode extends MessagesTreeBundleNode<MessagesTreeRootNode, ExplainPlanMessagesFileNode> {
    public ExplainPlanMessagesNode(MessagesTreeRootNode parent) {
        super(parent);
    }

    @Nullable
    private ExplainPlanMessagesFileNode getChildTreeNode(VirtualFile virtualFile) {
        for (ExplainPlanMessagesFileNode messagesTreeNode : getChildren()) {
            if (virtualFile.equals(messagesTreeNode.getFile())) {
                return messagesTreeNode;
            }
        }
        return null;
    }

    public TreePath addExplainPlanMessage(ExplainPlanMessage explainPlanMessage) {
        ExplainPlanMessagesFileNode node = getFileTreeNode(explainPlanMessage);
        if (node == null) {
            node = new ExplainPlanMessagesFileNode(this, explainPlanMessage.getVirtualFile());
            addChild(node);
            getTreeModel().notifyTreeModelListeners(this, TreeEventType.STRUCTURE_CHANGED);
        }
        return node.addExplainPlanMessage(explainPlanMessage);
    }


    @Nullable
    public TreePath getTreePath(ExplainPlanMessage explainPlanMessage) {
        ExplainPlanMessagesFileNode messagesFileNode = getFileTreeNode(explainPlanMessage);
        if (messagesFileNode != null) {
            return messagesFileNode.getTreePath(explainPlanMessage);
        }
        return null;
    }

    @Nullable
    private ExplainPlanMessagesFileNode getFileTreeNode(ExplainPlanMessage explainPlanMessage) {
        return getChildTreeNode(explainPlanMessage.getVirtualFile());
    }
}
