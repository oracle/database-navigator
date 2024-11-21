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

import com.dbn.execution.common.message.ui.tree.MessagesTreeLeafNode;
import com.dbn.execution.explain.result.ExplainPlanMessage;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

public class ExplainPlanMessageNode extends MessagesTreeLeafNode<ExplainPlanMessagesFileNode, ExplainPlanMessage> {

    ExplainPlanMessageNode(ExplainPlanMessagesFileNode parent, ExplainPlanMessage explainPlanMessage) {
        super(parent, explainPlanMessage);
    }

    @Nullable
    @Override
    public VirtualFile getFile() {
        return getParent().getFile();
    }

    @Override
    public String toString() {
        ExplainPlanMessage explainPlanMessage = getMessage();
        return
            explainPlanMessage.getText() + " - Connection: " +
            explainPlanMessage.getConnection().getName();
    }
}
