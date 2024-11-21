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

package com.dbn.connection;

import com.dbn.common.util.Enumerations;
import lombok.Getter;

@Getter
public enum ConnectionType{
    MAIN("Main", 0),
    POOL("Pool", 1),
    SESSION("Session", 2),
    DEBUG("Debug", 3),
    DEBUGGER("Debugger", 4),
    ASSISTANT("Assistant", 6),
    TEST("Test", 5),
    ;

    private final String name;
    private final int priority;

    ConnectionType(String name, int priority) {
        this.name = name;
        this.priority = priority;
    }

    public boolean isOneOf(ConnectionType... connectionTypes){
        return Enumerations.isOneOf(this, connectionTypes);
    }

    public boolean matches(ConnectionType... connectionTypes){
        return connectionTypes == null || connectionTypes.length == 0 || isOneOf(connectionTypes);
    }
}
