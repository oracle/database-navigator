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
