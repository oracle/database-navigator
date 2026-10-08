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

package com.dbn.migration.liquibase.execution;

import com.dbn.common.util.Strings;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.type.DBObjectType;
import liquibase.structure.DatabaseObject;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import static com.dbn.migration.liquibase.execution.LiquibaseDatabaseObjects.resolveObjectType;

/** Database object discovered while Liquibase creates a database snapshot. */
@Getter
public class LiquibaseSnapshotItem extends LiquibaseExecutionItem {
    private static final String DEFAULT_MESSAGE = "Reading database object";
    private DatabaseObject databaseObject;
    private DBObjectType objectType;

    public LiquibaseSnapshotItem(@NotNull DatabaseObject databaseObject) {
        this(databaseObject, DEFAULT_STATUS, DEFAULT_MESSAGE);
    }

    public LiquibaseSnapshotItem(
            @NotNull DatabaseObject databaseObject,
            @NotNull LiquibaseExecutionItemStatus status,
            @Nullable String message) {
        super(status, message);
        this.databaseObject = databaseObject;
        this.objectType = resolveObjectType(databaseObject);
    }

    @NotNull
    public DBObjectType getObjectType() {
        return objectType;
    }

    public boolean isSecondary() {
        if (objectType.isRootObject()) return false;
        if (objectType.isSchemaObject()) return false;

        return true;
    }

    @Nullable
    public DatabaseObject getContainerObject() {
        if (!isSecondary()) return null;

        DatabaseObject[] parentObjects = databaseObject.getContainingObjects();
        if (parentObjects == null) return null;

        for (DatabaseObject parentObject : parentObjects) {
            if (parentObject == null) continue;

            DBObjectType parentObjectType = resolveObjectType(parentObject);
            if (parentObjectType == DBObjectType.SCHEMA) continue;
            if (!parentObjectType.isSchemaObject() && !parentObjectType.isRootObject()) continue;
            return parentObject;
        }
        return null;
    }

    @Nullable
    public DBObject resolveBrowserObject(@NotNull DBSchema schema) {
        return resolveBrowserObject(
                databaseObject, getObjectType(),
                databaseObject.getName(), schema,
                Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    @Nullable
    private static DBObject resolveBrowserObject(
            DatabaseObject databaseObject,
            DBObjectType objectType,
            String objectName,
            DBSchema schema,
            Set<DatabaseObject> visited) {
        if (databaseObject == null) return null;
        if (objectName == null) return null;
        if (!visited.add(databaseObject)) return null;
        if (objectType == DBObjectType.UNKNOWN) return null;
        if (objectType == DBObjectType.SCHEMA) {
            return Strings.equalsIgnoreCase(schema.getName(), objectName) ? schema : null;
        }

        DatabaseObject[] parentObjects = databaseObject.getContainingObjects();
        if (parentObjects == null) parentObjects = new DatabaseObject[0];
        for (DatabaseObject parentObject : parentObjects) {
            if (parentObject == null) continue;

            String parentObjectName = parentObject.getName();
            DBObjectType parentObjectType = resolveObjectType(parentObject);
            DBObject parent = resolveBrowserObject(
                    parentObject,
                    parentObjectType,
                    parentObjectName,
                    schema,
                    visited);

            if (parent == null) continue;

            DBObject child = parent.getChildObject(objectType, objectName, true);
            if (child != null) return child;
        }

        return schema.getChildObject(objectType, objectName, true);
    }

    public void update(@NotNull DatabaseObject databaseObject, @NotNull LiquibaseExecutionItemStatus status, @Nullable String message) {
        this.databaseObject = databaseObject;
        this.objectType = resolveObjectType(databaseObject);
        updateStatus(status, message);
    }
}
