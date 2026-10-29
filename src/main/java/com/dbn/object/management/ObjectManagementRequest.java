/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.object.management;

import com.dbn.common.attributes.AttributeKey;
import com.dbn.common.attributes.Attributes;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.object.common.DBObject;
import lombok.experimental.Delegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Execution-scoped object management context and operation inputs.
 */
public record ObjectManagementRequest<T extends DBObject>(
        @NotNull @Delegate ConnectionHandler connection,
        @NotNull DBNConnection conn,
        @NotNull T object,
        @NotNull Attributes attributes) {
    public static final AttributeKey<String> NEW_NAME = new AttributeKey<>("OBJECT_MANAGEMENT_NEW_NAME", String.class);

    @Nullable
    public <V> V getAttribute(AttributeKey<V> key) {
        return attributes.get(key);
    }

    public <V> V getAttribute(AttributeKey<V> key, V defaultValue) {
        return attributes.get(key, defaultValue);
    }

    @NotNull
    public <V> V requireAttribute(AttributeKey<V> key) {
        return attributes.require(key);
    }

}
