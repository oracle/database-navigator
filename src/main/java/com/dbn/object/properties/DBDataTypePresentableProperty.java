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

package com.dbn.object.properties;

import com.dbn.data.type.DBDataType;
import com.dbn.object.DBType;
import com.intellij.pom.Navigatable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.swing.Icon;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class DBDataTypePresentableProperty extends PresentableProperty{
    private final DBDataType dataType;
    private String name = "Data type";

    public DBDataTypePresentableProperty(String name, DBDataType dataType) {
        this.name = name;
        this.dataType = dataType;
    }

    public DBDataTypePresentableProperty(DBDataType dataType) {
        this.dataType = dataType;
    }

    @Override
    public String getValue() {
        return dataType.getQualifiedName();
    }

    @Override
    public Icon getIcon() {
        DBType declaredType = dataType.getDeclaredType();
        return declaredType == null ? null : declaredType.getIcon();
    }

    @Override
    public Navigatable getNavigatable() {
        return dataType.getDeclaredType();
    }
}
