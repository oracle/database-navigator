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

package com.dbn.connection.config;

import com.dbn.common.routine.Consumer;
import com.dbn.connection.ConnectionId;
import com.intellij.util.messages.Topic;

import java.util.EventListener;

public interface ConnectionConfigListener extends EventListener {
    Topic<ConnectionConfigListener> TOPIC = Topic.create("Connection changed", ConnectionConfigListener.class);

    default void connectionsChanged() {}

    default void connectionRemoved(ConnectionId connectionId) {}

    default void connectionChanged(ConnectionId connectionId) {}

    default void connectionNameChanged(ConnectionId connectionId) {}

    static ConnectionConfigAdapter whenSetupChanged(Runnable consumer) {
        return new ConnectionConfigAdapter().whenSetupChanged(consumer);
    }

    static ConnectionConfigAdapter whenChanged(Consumer<ConnectionId> consumer) {
        return new ConnectionConfigAdapter().whenChanged(consumer);
    }

    static ConnectionConfigAdapter whenRemoved(Consumer<ConnectionId> consumer) {
        return new ConnectionConfigAdapter().whenRemoved(consumer);
    }

    static ConnectionConfigAdapter whenChangedOrRemoved(Consumer<ConnectionId> consumer) {
        return new ConnectionConfigAdapter().whenChanged(consumer).whenRemoved(consumer);
    }

    static ConnectionConfigAdapter whenNameChanged(Consumer<ConnectionId> consumer) {
        return new ConnectionConfigAdapter().whenNameChanged(consumer);
    }

}
