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
import com.dbn.common.dispose.Failsafe;
import com.dbn.connection.ConnectionId;
import com.dbn.execution.common.message.ConsoleMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreeNode;
import java.util.Enumeration;

public abstract class MessagesTreeLeafNode<P extends MessagesTreeNode, M extends ConsoleMessage> extends MessagesTreeNodeBase<P, MessagesTreeNode>{
    private final M message;

    protected MessagesTreeLeafNode(P parent, M message) {
        super(parent);
        this.message = message;
    }

    @Nullable
    @Override
    public ConnectionId getConnectionId() {
        return message.getConnectionId();
    }

    @NotNull
    public final M getMessage() {
        return Failsafe.nn(message);
    }

    /*********************************************************
     *                        TreeNode                       *
     *********************************************************/
    @Override
    public final TreeNode getChildAt(int childIndex) {
        return null;
    }

    @Override
    public final int getChildCount() {
        return 0;
    }

    @Override
    public final int getIndex(TreeNode node) {
        return -1;
    }

    @Override
    public final boolean getAllowsChildren() {
        return false;
    }

    @Override
    public final boolean isLeaf() {
        return true;
    }

    @Override
    public final Enumeration children() {
        return null;
    }


    @Override
    public void disposeInner() {
        Disposer.dispose(message);
        nullify();
    }
}
