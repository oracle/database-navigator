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

package com.dbn.object.event;

import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.object.type.DBObjectType;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EventListener;

public interface ObjectChangeListener extends EventListener {
    Topic<ObjectChangeListener> TOPIC = Topic.create("Object Change Event", ObjectChangeListener.class);

    void objectsChanged(
            @NotNull ConnectionId connectionId,
            @Nullable SchemaId ownerId,
            @NotNull DBObjectType objectType);
}
