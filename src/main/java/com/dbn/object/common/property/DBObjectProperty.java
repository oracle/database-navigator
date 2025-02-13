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

package com.dbn.object.common.property;

import com.dbn.common.property.Property;

public enum DBObjectProperty implements Property.LongBase {
    // generic
    TEMPORARY,
    NAVIGABLE,
    EDITABLE,
    COMPILABLE,
    DISABLEABLE,
    DEBUGABLE,
    INVALIDABLE,
    REFERENCEABLE,
    ROOT_OBJECT,
    SCHEMA_OBJECT,
    SYSTEM_OBJECT,

    DETERMINISTIC,
    COLLECTION,

    // schema
    USER_SCHEMA,
    EMPTY_SCHEMA,
    PUBLIC_SCHEMA,
    SYSTEM_SCHEMA,

    // column
    PRIMARY_KEY,
    FOREIGN_KEY,
    UNIQUE_KEY,
    IDENTITY,
    NULLABLE,
    HIDDEN,
    UNIQUE,

    // argument
    INPUT,
    OUTPUT,

    // user, privileges
    EXPIRED,
    LOCKED,
    ADMIN_OPTION,
    DEFAULT_ROLE,
    SESSION_USER,

    // trigger
    FOR_EACH_ROW,

    // java
    ABSTRACT,
    FINAL,
    STATIC,
    INNER,
    PRIMITIVE,

    // other

    // these belong to DBObjectStatus (here for optimization reasons)
    TREE_LOADED,
    LISTS_LOADED,
    DISPOSED,
    ;

    public static final DBObjectProperty[] VALUES = values();

    private final LongMasks masks = new LongMasks(this);

    @Override
    public LongMasks masks() {
        return masks;
    }
}
