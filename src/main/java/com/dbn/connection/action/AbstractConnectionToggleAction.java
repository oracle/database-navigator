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

package com.dbn.connection.action;

import com.dbn.common.action.ToggleAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public abstract class AbstractConnectionToggleAction extends ToggleAction {
    private final ConnectionRef connection;

    public AbstractConnectionToggleAction(String text, @NotNull ConnectionHandler connection) {
        this(text, null, connection);

    }
    public AbstractConnectionToggleAction(String text, Icon icon, @NotNull ConnectionHandler connection) {
        this(text, null, icon, connection);
    }
    public AbstractConnectionToggleAction(String text, String description, Icon icon, @NotNull ConnectionHandler connection) {
        super(text, description, icon);
        this.connection = connection.ref();
    }

    @NotNull
    public ConnectionHandler getConnection() {
        return connection.ensure();
    }
}
