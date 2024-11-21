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

package com.dbn.data.editor.ui;

import com.dbn.connection.ConnectionHandler;
import com.dbn.data.editor.text.TextContentType;
import com.dbn.data.type.DBDataType;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public interface UserValueHolder<T> {
    void setUserValue(T userValue);
    void updateUserValue(T userValue, boolean bulk);
    TextContentType getContentType();
    void setContentType(TextContentType contentType);
    T getUserValue();
    String getPresentableValue();
    String getName();
    DBDataType getDataType();
    DBObjectType getObjectType();
    Project getProject();

    @NotNull
    default ConnectionHandler getConnection() {
        throw new UnsupportedOperationException();
    }
}
