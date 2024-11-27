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
import com.dbn.execution.statement.StatementExecutionMessage;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreePath;

public class StatementExecutionMessagesFileNode extends MessagesTreeBundleNode<StatementExecutionMessagesNode, StatementExecutionMessageNode> {
    private final VirtualFileRef file;

    StatementExecutionMessagesFileNode(StatementExecutionMessagesNode parent, VirtualFile file) {
        super(parent);
        this.file = VirtualFileRef.of(file);
    }

    @NotNull
    @Override
    public VirtualFile getFile() {
        return VirtualFileRef.ensure(file);
    }

    TreePath addExecutionMessage(StatementExecutionMessage executionMessage) {
        StatementExecutionMessageNode execMessageNode = new StatementExecutionMessageNode(this, executionMessage);
        addChild(execMessageNode);
        getTreeModel().notifyTreeModelListeners(this, TreeEventType.STRUCTURE_CHANGED);
        return Trees.createTreePath(execMessageNode);
    }

    @Nullable
    public TreePath getTreePath(StatementExecutionMessage executionMessage) {
        for (StatementExecutionMessageNode messageNode : getChildren()) {
            if (messageNode.getMessage() == executionMessage) {
                return Trees.createTreePath(messageNode);
            }
        }
        return null;
    }
}
