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

package com.dbn.execution.explain.result;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.message.MessageType;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.execution.common.message.ConsoleMessage;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

public class ExplainPlanMessage extends ConsoleMessage {

    @Getter
    private final ExplainPlanResult explainPlanResult;

    public ExplainPlanMessage(ExplainPlanResult explainPlanResult, MessageType messageType) {
        super(messageType, explainPlanResult.getErrorMessage());
        this.explainPlanResult = explainPlanResult;

        Disposer.register(this, explainPlanResult);
    }

    @Nullable
    @Override
    public ConnectionId getConnectionId() {
        return explainPlanResult.getConnectionId();
    }

    public VirtualFile getVirtualFile() {
        return explainPlanResult.getVirtualFile();
    }

    @Deprecated
    public void navigateToEditor(boolean requestFocus) {
        //executionResult.getExecutionProcessor().navigateToEditor(requestFocus);
    }

    public ConnectionHandler getConnection() {
        return explainPlanResult.getConnection();
    }
}
