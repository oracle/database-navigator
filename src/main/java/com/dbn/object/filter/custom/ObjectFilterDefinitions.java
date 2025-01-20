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

import com.dbn.object.DBColumn;
import com.dbn.object.DBConstraint;
import com.dbn.object.DBIndex;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.DBView;
import com.dbn.object.common.DBObject;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static com.dbn.common.util.Unsafe.cast;

@NonNls
class ObjectFilterDefinitions {
    private static final Map<DBObjectType, ObjectFilterDefinition> REGISTRY = new HashMap<>();

    @NotNull
    static <T extends DBObject> ObjectFilterDefinition<T> attributesOf(DBObjectType objectType) {
        return cast(REGISTRY.computeIfAbsent(objectType, t -> createDefault(t)));
    }

    private static @NotNull ObjectFilterDefinitionImpl<DBObject> createDefault(DBObjectType objectType) {
        String nameAttribute = objectType.name() + "_NAME";
        return new ObjectFilterDefinitionImpl<>(objectType, nameAttribute + " like 'OBJ_%'")
                .withAttribute(String.class, nameAttribute, objectType.getName()+ " name", o -> o.getName());
    }

    static {
        create(DBSchema.class, DBObjectType.SCHEMA, "SCHEMA_NAME not in ('SCH1', 'SCH2') and SCHEMA_NAME not like 'TEMP_%' and not SYSTEM_SCHEMA")
                .withAttribute(String.class,  "SCHEMA_NAME",     "schema name", o -> o.getName())
                .withAttribute(Boolean.class, "USER_SCHEMA",     "is user / own schema", o -> o.isUserSchema())
                .withAttribute(Boolean.class, "PUBLIC_SCHEMA",   "is public schema", o -> o.isPublicSchema())
                .withAttribute(Boolean.class, "SYSTEM_SCHEMA",   "is system schema", o -> o.isSystemSchema())
                .withAttribute(Boolean.class, "EMPTY_SCHEMA",    "is empty schema", o -> o.isEmptySchema());

        create(DBTable.class, DBObjectType.TABLE, "TABLE_NAME in ('TBL1', 'TBL2') or TABLE_NAME like 'TAB_%' or TEMPORARY_TABLE")
                .withAttribute(String.class,  "TABLE_NAME",      "table name", o -> o.getName())
                .withAttribute(Boolean.class, "TEMPORARY_TABLE", "is temporary table", o -> o.isTemporary());

        create(DBView.class, DBObjectType.VIEW, "VIEW_NAME in ('VIEW1', 'VIEW2') or VIEW_NAME like 'VW_%' or SYSTEM_VIEW")
                .withAttribute(String.class,  "VIEW_NAME",       "view name", o -> o.getName())
                .withAttribute(Boolean.class, "SYSTEM_VIEW",     "is system level view", o -> o.isSystemView());

        create(DBColumn.class, DBObjectType.COLUMN, "COLUM_NAME in ('COL1', 'COL2') or COLUM_NAME like 'COL_%' or PRIMARY_KEY")
                .withAttribute(String.class,  "COLUM_NAME",      "column name", o -> o.getName())
                .withAttribute(String.class,  "DATA_TYPE",       "column data name", o -> o.getDataType().getName())
                .withAttribute(Integer.class, "DATA_LENGTH",     "column data length", o -> o.getDataType().getLength())
                .withAttribute(Boolean.class, "AUDIT_COLUMN",    "is audit / tracking column", o -> o.isAudit())
                .withAttribute(Boolean.class, "PSEUDO_COLUMN",   "is pseudo / hidden column", o -> o.isHidden())
                .withAttribute(Boolean.class, "NULLABLE_COLUMN", "is nullable column", o -> o.isNullable())
                .withAttribute(Boolean.class, "IDENTITY_COLUMN", "is identity column", o -> o.isIdentity())
                .withAttribute(Boolean.class, "PRIMARY_KEY",     "is primary key column", o -> o.isPrimaryKey())
                .withAttribute(Boolean.class, "FOREIGN_KEY",     "is foreign key column", o -> o.isForeignKey());

        create(DBConstraint.class, DBObjectType.CONSTRAINT, "CONSTRAINT_NAME in ('CONS1', 'CONS2') or CONSTRAINT_NAME like 'USRC_%' or UNIQUE_KEY")
                .withAttribute(String.class,  "CONSTRAINT_NAME", "constraint name", o -> o.getName())
                .withAttribute(Boolean.class, "PRIMARY_KEY",     "is primary key column", o -> o.isPrimaryKey())
                .withAttribute(Boolean.class, "FOREIGN_KEY",     "is foreign key column", o -> o.isForeignKey())
                .withAttribute(Boolean.class, "UNIQUE_KEY",      "is unique key column", o -> o.isUniqueKey());

        create(DBIndex.class, DBObjectType.INDEX, "INDEX_NAME in ('IDX1', 'IDX2') or INDEX_NAME like 'IDX_%' or UNIQUE_INDEX")
                .withAttribute(String.class,  "INDEX_NAME",      "index name", o -> o.getName())
                .withAttribute(Boolean.class, "UNIQUE_INDEX",    "is unique index", o -> o.isUnique());

    }

    private static <T extends DBObject> ObjectFilterDefinitionImpl<T> create(Class<T> objectClass, DBObjectType objectType, String example) {
        ObjectFilterDefinitionImpl<T> attributes = new ObjectFilterDefinitionImpl<>(objectType, example);
        REGISTRY.put(objectType, attributes);
        return attributes;
    }
}
