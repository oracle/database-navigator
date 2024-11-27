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

package com.dbn.vfs;

import com.dbn.common.environment.EnvironmentTypeProvider;
import com.dbn.connection.context.DatabaseContextBase;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public interface DBVirtualFile extends /*VirtualFileWithId, */EnvironmentTypeProvider, DatabaseContextBase, UserDataHolder {
    @NotNull
    Project getProject();

    Icon getIcon();

    void setCachedViewProvider(@Nullable DatabaseFileViewProvider viewProvider);

    @Nullable
    DatabaseFileViewProvider getCachedViewProvider();

    void invalidate();

    @Nullable
    default DBObjectRef getObjectRef() {
        return null;
    }

    @Nullable
    default DBObject getObject() {
        return null;
    }

    @Nullable
    default DBObjectType getObjectType() {
        return null;
    }
}