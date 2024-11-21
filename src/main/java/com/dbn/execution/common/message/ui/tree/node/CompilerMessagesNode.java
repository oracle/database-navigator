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
import com.dbn.execution.common.message.ui.tree.MessagesTreeNode;
import com.dbn.execution.common.message.ui.tree.MessagesTreeRootNode;
import com.dbn.execution.compiler.CompilerMessage;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreePath;

public class CompilerMessagesNode extends MessagesTreeBundleNode<MessagesTreeRootNode, CompilerMessagesObjectNode> {
    public CompilerMessagesNode(MessagesTreeRootNode parent) {
        super(parent);
    }

    @Nullable
    private MessagesTreeNode getChildTreeNode(VirtualFile virtualFile) {
        for (MessagesTreeNode messagesTreeNode : getChildren()) {
            VirtualFile nodeVirtualFile = messagesTreeNode.getFile();
            if (nodeVirtualFile != null && nodeVirtualFile.equals(virtualFile)) {
                return messagesTreeNode;
            }
        }
        return null;
    }

    public TreePath addCompilerMessage(CompilerMessage compilerMessage) {
        DBEditableObjectVirtualFile databaseFile = compilerMessage.getDatabaseFile();
        CompilerMessagesObjectNode objectNode = (CompilerMessagesObjectNode) getChildTreeNode(databaseFile);
        if (objectNode == null) {
            DBObjectRef<DBSchemaObject> objectRef = compilerMessage.getCompilerResult().getObjectRef();
            objectNode = new CompilerMessagesObjectNode(this, objectRef);
            addChild(objectNode);
            getTreeModel().notifyTreeModelListeners(this, TreeEventType.STRUCTURE_CHANGED);
        }
        return objectNode.addCompilerMessage(compilerMessage);
    }

    @Nullable
    public TreePath getTreePath(CompilerMessage compilerMessage) {
        DBEditableObjectVirtualFile databaseFile = compilerMessage.getDatabaseFile();
        CompilerMessagesObjectNode objectNode = (CompilerMessagesObjectNode) getChildTreeNode(databaseFile);
        if (objectNode != null) {
            return objectNode.getTreePath(compilerMessage);
        }
        return null;
    }
}