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

package com.dbn.object.filter.custom;

import com.dbn.code.common.lookup.CodeCompletionLookupItem;
import com.intellij.codeInsight.lookup.LookupItem;
import lombok.Value;
import org.jetbrains.annotations.NonNls;

@NonNls
@Value
public class ObjectFilterAttribute {
    private final Class type;
    private final String name;
    private final String description;
    private final transient LookupItem lookupItem;

    public ObjectFilterAttribute(Class type, String name, String description) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.lookupItem = new CodeCompletionLookupItem(this, null, name, description  + " (" +getTypeName() + ")", false);
    }

    public Object getTestValue() {
        if (boolean.class.isAssignableFrom(type)) return false;
        if (Boolean.class.isAssignableFrom(type)) return false;
        if (String.class.isAssignableFrom(type)) return "";
        if (Short.class.isAssignableFrom(type)) return 0;
        if (Integer.class.isAssignableFrom(type)) return 0;
        if (Long.class.isAssignableFrom(type)) return 0L;
        if (Float.class.isAssignableFrom(type)) return 0.0f;
        if (Double.class.isAssignableFrom(type)) return 0.0;
        if (Byte.class.isAssignableFrom(type)) return (byte) 0;
        if (Character.class.isAssignableFrom(type)) return ' ';
        if (Enum.class.isAssignableFrom(type)) return null;
        return null;
    }

    public String getTypeName() {
        if (boolean.class.isAssignableFrom(type)) return "boolean";
        if (Boolean.class.isAssignableFrom(type)) return "boolean";
        if (String.class.isAssignableFrom(type)) return "literal";
        if (Short.class.isAssignableFrom(type)) return "numeric";
        if (Integer.class.isAssignableFrom(type)) return "numeric";
        if (Long.class.isAssignableFrom(type)) return "numeric";
        if (Float.class.isAssignableFrom(type)) return "numeric";
        if (Double.class.isAssignableFrom(type)) return "numeric";
        if (Byte.class.isAssignableFrom(type)) return "numeric";
        if (Character.class.isAssignableFrom(type)) return "literal";
        if (Enum.class.isAssignableFrom(type)) return "literal";
        return null;
    }

}
