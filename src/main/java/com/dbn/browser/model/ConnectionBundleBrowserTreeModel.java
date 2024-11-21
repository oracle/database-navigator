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
import com.dbn.connection.ConnectionBundle;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionHandlerStatusListener;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConnectionBundleBrowserTreeModel extends BrowserTreeModel {
    public ConnectionBundleBrowserTreeModel(@NotNull Project project, @Nullable ConnectionBundle connectionBundle) {
        //super(new ConnectionBundleBrowserTreeRoot(project, connectionBundle));
        super(connectionBundle);
        ProjectEvents.subscribe(project, this, ConnectionHandlerStatusListener.TOPIC, connectionHandlerStatusListener());
    }

    @Override
    public boolean contains(BrowserTreeNode node) {
        return true;
    }

    @NotNull
    private ConnectionHandlerStatusListener connectionHandlerStatusListener() {
        return (connectionId) -> {
            ConnectionHandler connection = ConnectionHandler.get(connectionId);
            if (connection == null) return;

            notifyListeners(connection.getObjectBundle(), TreeEventType.NODES_CHANGED);
        };
    }
}
