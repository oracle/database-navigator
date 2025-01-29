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

import com.dbn.object.DBSchema;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an abstract input definition for creating schema-based objects.
 * This class extends {@link ObjectFactoryInput} and encapsulates the schema
 * information referenced by {@link DBSchema}, along with specific object type
 * and name details required for object creation.
 *
 * @author Dan Cioca (Oracle)
 */
abstract class SchemaObjectFactoryInput extends ObjectFactoryInput{
    private final DBObjectRef<DBSchema> schema;

    protected SchemaObjectFactoryInput(DBSchema schema, String objectName, DBObjectType objectType) {
        super(objectName, objectType, null, 0);
        this.schema = DBObjectRef.of(schema);
    }

    @NotNull
    public final DBSchema getSchema() {
        return DBObjectRef.ensure(schema);
    }

    @Override
    public String getObjectPath() {
        return schema.getObjectName() + "." + super.getObjectPath();
    }
}
