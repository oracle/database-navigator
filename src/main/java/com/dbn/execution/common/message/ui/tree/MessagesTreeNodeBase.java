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

import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.ref.WeakRef;

public abstract class MessagesTreeNodeBase<P extends MessagesTreeNode, C extends MessagesTreeNode>
        extends StatefulDisposableBase
        implements MessagesTreeNode<P, C>{

    private final WeakRef<P> parent;

    MessagesTreeNodeBase(P parent) {
        this.parent = WeakRef.of(parent);
    }

    @Override
    public P getParent() {
        return WeakRef.get(parent);
    }
}
