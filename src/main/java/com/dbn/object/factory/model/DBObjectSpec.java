/*
 * Copyright 2025 Oracle and/or its affiliates
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

import com.dbn.common.data.Data;
import com.dbn.language.common.QuotePair;
import com.dbn.object.DBSchema;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.object.factory.model.DBObjectAttributeType.OBJECT_NAME;
import static com.dbn.object.factory.model.DBObjectAttributeType.OBJECT_TYPE;

@Getter
@Setter
public class DBObjectSpec extends DBObjectSpecBase{
    private boolean readonly;

    private final Map<DBObjectType, DBObjectSpecList<DBObjectSpec>> children = new EnumMap<>(DBObjectType.class);
    private final Map<DBObjectAttributeType, DBObjectAttribute> attributes = new HashMap<>();

    public DBObjectSpec(DBObjectSpec parent) {
        super(parent);
    }

    public DBObjectSpec(DBObjectSpec parent, DBObjectType objectType) {
        this(parent);
        setObjectType(objectType);
    }

    public DBObjectSpec(DBSchema schema) {
        super(null);
        setConnectionId(schema.getConnectionId());
        setSchemaId(schema.getSchemaId());
    }

    public DBObjectSpec(DBSchema schema, DBObjectType objectType) {
        this(schema);
        setObjectType(objectType);
    }

    @Nullable
    public <T> String getStringAttributeValue(DBObjectAttributeType<T> type) {
        T value = getAttributeValue(type);
        return Data.asString(value);
    }

    public boolean getBooleanAttributeValue(DBObjectAttributeType<Boolean> type) {
        Boolean value = getAttributeValue(type);
        return value != null && value;
    }

    public void addChild(DBObjectSpec child) {
        DBObjectType objectType = child.getObjectType();
        List<DBObjectSpec> children = getChildren(objectType);
        children.add(child);
    }

    public void setChildrenReadonly(DBObjectType objectType, boolean readonly) {
        getChildren(objectType).setReadonly(readonly);
    }

    public DBObjectSpecList<DBObjectSpec> getChildren(DBObjectType type) {
        return this.children.computeIfAbsent(type, t -> new DBObjectSpecList<>(this));
    }

    public int getIndex() {
        DBObjectSpec parent = getParent();
        if (parent == null) return 0;

        DBObjectSpecList<DBObjectSpec> children = parent.getChildren(getObjectType());
        return children.indexOf(this);
    }

    public String getObjectPath() {
        return getObjectName();
    }

    public void setObjectType(DBObjectType objectType) {
        setAttributeValue(OBJECT_TYPE, objectType);
    }

    public DBObjectType getObjectType() {
        return getAttributeValue(OBJECT_TYPE);
    }

    public String getObjectTypeName() {
        return getObjectType().getName();
    }

    public void setObjectName(String objectName) {
        setAttributeValue(OBJECT_NAME, objectName);
    }

    public String getObjectName() {
        return getAttributeValue(OBJECT_NAME);
    }

    public String getObjectName(boolean quoted) {
        String objectName = getObjectName();
        if (!quoted) return objectName;

        QuotePair quotes = getConnection().getCompatibilityInterface().getDefaultIdentifierQuotes();
        return quotes.quote(objectName);
    }

    public String getObjectDescription() {
        return getObjectTypeName() + " \"" + getObjectPath() + "\"";
    }

    @Override
    public String toString() {
        return getObjectTypeName() + " " + getObjectName();
    }

    public <T> DBObjectAttribute<T> getAttribute(DBObjectAttributeType<T> type) {
        return cast(attributes.get(type));
    }

    public <T> T getAttributeValue(DBObjectAttributeType<T> type) {
        DBObjectAttribute<T> attribute = getAttribute(type);
        return attribute == null ? null : attribute.getValue();
    }

    public <T> DBObjectAttribute<T> setAttributeValue(DBObjectAttributeType<T> type, T value) {
        DBObjectAttribute<T> attribute = cast(attributes.computeIfAbsent(type, t -> new DBObjectAttribute<>(this)));
        attribute.setValue(value);
        return attribute;
    }

}
