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

import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.message.MessageType;
import com.dbn.connection.ConnectionId;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreeNode;
import java.util.Collections;
import java.util.List;

public interface MessagesTreeNode<P extends MessagesTreeNode, C extends MessagesTreeNode> extends TreeNode, StatefulDisposable {
    P getParent();

    default MessagesTreeModel getTreeModel() {
        return getParent().getTreeModel();
    }

    @Nullable
    default VirtualFile getFile() {return null;}

    default List<C> getChildren() {return Collections.emptyList();}

    default boolean hasMessageChildren(MessageType type) {return false;}

    default void removeMessages(@NotNull ConnectionId connectionId) {};

    @Nullable
    default ConnectionId getConnectionId() {return null;};
}
