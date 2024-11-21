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

package com.dbn.object.factory;

import com.dbn.object.type.DBObjectType;
import lombok.Getter;

import java.util.List;

@Getter
public abstract class ObjectFactoryInput {
    private final String objectName;
    private final DBObjectType objectType;
    private final ObjectFactoryInput parent;
    private final int index;

    protected ObjectFactoryInput(String objectName, DBObjectType objectType, ObjectFactoryInput parent, int index) {
        this.objectName = objectName == null ? "" : objectName.trim();
        this.objectType = objectType;
        this.parent = parent;
        this.index = index;
    }

    public abstract void validate(List<String> errors);
}
