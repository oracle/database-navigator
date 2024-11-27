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

package com.dbn.execution.common.message;

import com.dbn.common.message.Message;
import com.dbn.common.message.MessageType;
import com.dbn.connection.ConnectionId;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

public abstract class ConsoleMessage extends Message implements Disposable {
    private boolean isNew = true;

    public ConsoleMessage(MessageType type, String text) {
        super(type, text);
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    @Nullable
    public ConnectionId getConnectionId() {
        return null;
    }

    @Override
    public void disposeInner() {
        nullify();
    }
}
