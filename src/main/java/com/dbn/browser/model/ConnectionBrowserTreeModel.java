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

package com.dbn.browser.model;

import com.dbn.common.event.ProjectEvents;
import com.dbn.common.ui.tree.TreeEventType;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionHandlerStatusListener;
import org.jetbrains.annotations.NotNull;

public class ConnectionBrowserTreeModel extends BrowserTreeModel {
    public ConnectionBrowserTreeModel(ConnectionHandler connection) {
        super(connection.getObjectBundle());
        ProjectEvents.subscribe(connection.getProject(), this, ConnectionHandlerStatusListener.TOPIC, connectionHandlerStatusListener());
    }

    @Override
    public boolean contains(BrowserTreeNode node) {
        return getConnection() == node.getConnection();
    }

    public ConnectionHandler getConnection() {
        return getRoot().getConnection();
    }

    @NotNull
    private ConnectionHandlerStatusListener connectionHandlerStatusListener() {
        return (connectionId) -> {
            ConnectionHandler connection = getConnection();
            if (connection.getConnectionId() == connectionId) {
                notifyListeners(connection.getObjectBundle(), TreeEventType.NODES_CHANGED);
            }
        };
    }
}
