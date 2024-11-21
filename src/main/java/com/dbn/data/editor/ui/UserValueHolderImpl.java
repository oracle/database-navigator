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

import com.dbn.common.project.ProjectRef;
import com.dbn.data.editor.text.TextContentType;
import com.dbn.data.type.DBDataType;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;

public class UserValueHolderImpl<T> implements UserValueHolder<T>{
    private final String name;
    private final DBDataType dataType;
    private final DBObjectType objectType;
    private final ProjectRef project;
    private T userValue;
    private TextContentType contentType;

    public UserValueHolderImpl(String name, DBObjectType objectType, DBDataType dataType, Project project) {
        this.name = name;
        this.objectType = objectType;
        this.dataType = dataType;
        this.project = ProjectRef.of(project);
    }

    @Override
    public T getUserValue() {
        return userValue;
    }

    @Override
    public String getPresentableValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUserValue(T userValue) {
        this.userValue = userValue;
    }

    @Override
    public void updateUserValue(T userValue, boolean bulk) {
        this.userValue = userValue;
    }

    @Override
    public TextContentType getContentType() {
        return contentType;
    }

    @Override
    public void setContentType(TextContentType contentType) {
        this.contentType = contentType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public DBObjectType getObjectType() {
        return objectType;
    }

    @Override
    public DBDataType getDataType() {
        return dataType;
    }

    @Override
    public Project getProject() {
        return project.ensure();
    }
}
