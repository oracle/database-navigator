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

import com.dbn.common.content.DynamicContent;
import com.dbn.common.content.DynamicContentType;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.ui.Presentable;
import com.dbn.common.util.Unsafe;
import com.dbn.connection.context.DatabaseContextBase;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectBundle;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.exception.Exceptions.unsupported;

public interface DatabaseEntity extends DatabaseContextBase, StatefulDisposable, Presentable {

    default String getQualifiedName() {
        return getName();
    }

    default String getQualifiedName(boolean quoted) {
        return quoted ? unsupported() : getName();
    }

    @NotNull
    Project getProject();

    @Nullable
    default <E extends DatabaseEntity> E getParentEntity() {
        return null;
    }

    //@NotNull
    default <E extends DatabaseEntity> E ensureParentEntity() {
        return Failsafe.nn(getParentEntity());
    }

    @Nullable
    default <E extends DatabaseEntity> E getUndisposedEntity() {
        return Unsafe.cast(this);
    }

    @Nullable
    default DynamicContent<?> getDynamicContent(DynamicContentType<?> dynamicContentType) {
        return null;
    }

    default DynamicContentType<?> getDynamicContentType() {
        return null;
    }

    @NotNull
    @Override
    default ConnectionHandler getConnection() {
        return unsupported();
    }

    default boolean isObject() {
        return this instanceof DBObject;
    }

    default boolean isObjectBundle() {
        return this instanceof DBObjectBundle;
    }
}
