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

package com.dbn.object.factory.model;

import com.dbn.database.DatabaseObjectTypeId;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Database-specific factory capabilities for one database object type.
 *
 * <p>{@link DBObjectSpec} contains the values entered by the user. This type
 * describes which attributes and values are valid for those inputs.</p>
 */
@Getter
public class DBObjectTypeSpec {
    private final DatabaseObjectTypeId objectTypeId;
    private final List<DBObjectAttributeSpec> attributes = new ArrayList<>();

    public DBObjectTypeSpec(DatabaseObjectTypeId objectTypeId) {
        this.objectTypeId = objectTypeId;
    }

    public static DBObjectTypeSpec create(DatabaseObjectTypeId objectTypeId) {
        return new DBObjectTypeSpec(objectTypeId);
    }

    public Set<DBObjectAttributeSpec> getAttributes() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(attributes));
    }

    public <T> DBObjectTypeSpec withAttribute(
            DBObjectAttributeType<T> type,
            T value,
            boolean required) {
        List<T> values = value == null ? List.of() : List.of(value);
        return withAttribute(type, values, false, required);
    }

    public <T> DBObjectTypeSpec withAttribute(
            DBObjectAttributeType<T> type,
            List<T> values,
            boolean multiple,
            boolean required) {

        DBObjectAttributeSpec attribute = new DBObjectAttributeSpec(type, values, multiple, required);
        attributes.removeIf(existing -> existing.type() == attribute.type());
        attributes.add(attribute);
        return this;
    }

    @Nullable
    public DBObjectAttributeSpec getAttribute(DBObjectAttributeType<?> type) {
        for (DBObjectAttributeSpec attribute : attributes) {
            if (attribute.type() == type) return attribute;
        }
        return null;
    }

    public boolean supports(DBObjectAttributeType<?> type) {
        return getAttribute(type) != null;
    }

    public boolean requires(DBObjectAttributeType<?> type) {
        DBObjectAttributeSpec attribute = getAttribute(type);
        return attribute != null && attribute.required();
    }

    public <T> boolean requires(DBObjectAttributeType<T> type, T value) {
        List<T> values = getAttributeValues(type);
        return requires(type) && values.size() == 1 && Objects.equals(values.get(0), value);
    }

    public <T> List<T> getAttributeValues(DBObjectAttributeType<T> type) {
        DBObjectAttributeSpec attribute = getAttribute(type);
        if (attribute == null || attribute.values().isEmpty()) return List.of();

        Class<T> valueType = type.getType();

        List<T> values = new ArrayList<>();
        for (Object value : attribute.values()) {
            values.add(valueType.cast(value));
        }
        return List.copyOf(values);
    }

    public boolean allowsMultiple(DBObjectAttributeType<?> type) {
        DBObjectAttributeSpec attribute = getAttribute(type);
        return attribute != null && attribute.multiple();
    }
}
