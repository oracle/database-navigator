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

package com.dbn.common.ui.component;

import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.project.ProjectSupplier;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionRef;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.UserDataHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

import static com.dbn.common.action.UserDataKeys.CONNECTION_REF;
import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.dispose.Failsafe.nn;

public interface DBNComponent extends StatefulDisposable, ProjectSupplier, UserDataHolder {
    @Nullable
    <T extends Disposable> T getParentComponent();

    @NotNull
    default <T extends Disposable> T ensureParentComponent() {
        return nn(getParentComponent());
    }

    @NotNull
    JComponent getComponent();


    @Nullable
    default ConnectionId getConnectionId() {
        ConnectionHandler connection = getConnection();
        return connection == null ? null : connection.getConnectionId();
    }

    @NotNull
    default ConnectionId ensureConnectionId() {
        return nn(getConnectionId());
    }

    @Nullable
    default ConnectionHandler getConnection() {
        ConnectionRef connection = getUserData(CONNECTION_REF);
        return ConnectionRef.get(connection);
    }

    @NotNull
    default ConnectionHandler ensureConnection() {
        return nd(getConnection());
    }

    default void setConnection(ConnectionHandler connection) {
        putUserData(CONNECTION_REF, ConnectionRef.of(connection));
    }

    default void inheritConnection(DBNComponent component) {
        setConnection(component.getConnection());
    }
}
