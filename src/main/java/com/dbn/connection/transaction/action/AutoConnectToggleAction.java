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

package com.dbn.connection.transaction.action;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.action.AbstractConnectionToggleAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class AutoConnectToggleAction extends AbstractConnectionToggleAction {

    public AutoConnectToggleAction(ConnectionHandler connection) {
        super("Connect Automatically", connection);

    }
    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        return getConnection().getSettings().getDetailSettings().isConnectAutomatically();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        getConnection().getSettings().getDetailSettings().setConnectAutomatically(state);
    }
}
