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

import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.pom.Navigatable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

@Getter
@EqualsAndHashCode(callSuper = false)
public class DBObjectPresentableProperty extends DBObjectPropertyBase {
    private final DBObjectRef<?> object;
    private final boolean qualified;
    private final String name;


    public DBObjectPresentableProperty(String name, @Nullable DBObject object, boolean qualified) {
        this.object = DBObjectRef.of(object);
        this.qualified = qualified;
        this.name = name;
    }

    public DBObjectPresentableProperty(DBObject object) {
        this(null, object, false);
    }

    @Override
    public String getName() {
        if (object == null) return name == null ? "Undefined" : name;

        return name == null ? object.getObjectType().getTitleCasedName() : name;
    }

    @Override
    public String getValue() {
        if (object == null) return "";

        return qualified ? object.getPath() : object.getObjectName();
    }

    @Override
    public Icon getIcon() {
        if (object == null) return null;

        DBObject object = this.object.get();
        return object == null ? null : object.getIcon();
    }

    @Override
    public Navigatable getNavigatable() {
        return DBObjectRef.get(object);
    }
}
