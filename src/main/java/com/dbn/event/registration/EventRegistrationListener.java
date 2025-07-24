/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.event.registration;

import com.dbn.connection.ConnectionId;
import com.dbn.object.event.ObjectChangeAction;
import com.intellij.util.messages.Topic;
import lombok.Getter;

import java.util.EventListener;

public interface EventRegistrationListener extends EventListener {
    Topic<EventRegistrationListener> TOPIC = Topic.create("Data Change Registration Event", EventRegistrationListener.class);

    void registrationsChanged(RegistrationEvent event);

    static RegistrationEvent event(ConnectionId connectionId, ObjectChangeAction action) {
        return new RegistrationEvent(connectionId, action);
    }

    @Getter
    class RegistrationEvent {
        private final ConnectionId connectionId;
        private final ObjectChangeAction action;

        public RegistrationEvent(ConnectionId connectionId, ObjectChangeAction action) {
            this.connectionId = connectionId;
            this.action = action;
        }
    }
}
