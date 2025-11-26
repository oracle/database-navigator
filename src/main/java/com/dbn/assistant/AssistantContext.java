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

package com.dbn.assistant;

import com.dbn.connection.ConnectionId;
import lombok.Data;

import java.util.Objects;

@Data
public class AssistantContext {
    private final ConnectionId connectionId;
    private final AssistantType assistantType;

    public AssistantContext(ConnectionId connectionId, AssistantType assistantType) {
        this.connectionId = connectionId;
        this.assistantType = assistantType;
    }

    public AssistantContext(String identifier) {
        String[] tokens = identifier.split("@");
        assistantType = AssistantType.valueOf(tokens[0]);
        connectionId = ConnectionId.get(tokens[1]);
    }

    public String getIdentifier() {
        return assistantType + "@" + connectionId;
    }

    public boolean matches(ConnectionId connectionId, AssistantType assistantType) {
        return
            Objects.equals(this.connectionId, connectionId) &&
            Objects.equals(this.assistantType, assistantType);
    }

    public static AssistantContext fromIdentifier(String identifier) {
        return new AssistantContext(identifier);
    }

    @Override
    public String toString() {
        return getIdentifier();
    }
}
