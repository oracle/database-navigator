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

import com.dbn.common.file.VirtualFileRef;
import com.dbn.common.ui.tree.TreeEventType;
import com.dbn.common.ui.tree.Trees;
import com.dbn.execution.common.message.ui.tree.MessagesTreeBundleNode;
import com.dbn.execution.explain.result.ExplainPlanMessage;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreePath;

public class ExplainPlanMessagesFileNode extends MessagesTreeBundleNode<ExplainPlanMessagesNode, ExplainPlanMessageNode> {
    private final VirtualFileRef file;

    ExplainPlanMessagesFileNode(ExplainPlanMessagesNode parent, VirtualFile file) {
        super(parent);
        this.file = VirtualFileRef.of(file);
    }

    @NotNull
    @Override
    public VirtualFile getFile() {
        return VirtualFileRef.ensure(file);
    }

    TreePath addExplainPlanMessage(ExplainPlanMessage explainPlanMessage) {
        ExplainPlanMessageNode explainPlanMessageNode = new ExplainPlanMessageNode(this, explainPlanMessage);
        addChild(explainPlanMessageNode);
        getTreeModel().notifyTreeModelListeners(this, TreeEventType.STRUCTURE_CHANGED);
        return Trees.createTreePath(explainPlanMessageNode);
    }

    @Nullable
    public TreePath getTreePath(ExplainPlanMessage explainPlanMessage) {
        for (ExplainPlanMessageNode messageNode : getChildren()) {
            if (messageNode.getMessage() == explainPlanMessage) {
                return Trees.createTreePath(messageNode);
            }
        }
        return null;
    }
}
