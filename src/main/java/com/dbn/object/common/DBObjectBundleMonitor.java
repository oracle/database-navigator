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

package com.dbn.object.common;

import com.dbn.common.ref.WeakRef;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.object.DBSchema;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.common.list.DBObjectListContainer;
import com.dbn.object.event.ObjectChangeAction;
import com.dbn.object.event.ObjectChangeEvent;
import com.dbn.object.event.ObjectChangeListener;
import com.dbn.object.type.DBObjectType;
import lombok.extern.slf4j.Slf4j;

import static com.dbn.common.dispose.Failsafe.nn;
import static com.dbn.object.event.ObjectChangeAction.CREATE;
import static com.dbn.object.event.ObjectChangeAction.DELETE;
import static com.dbn.object.event.ObjectChangeAction.DISABLE;
import static com.dbn.object.event.ObjectChangeAction.ENABLE;
import static com.dbn.object.event.ObjectChangeAction.LOCK;
import static com.dbn.object.event.ObjectChangeAction.REFRESH;
import static com.dbn.object.event.ObjectChangeAction.RENAME;
import static com.dbn.object.event.ObjectChangeAction.UNLOCK;
import static com.dbn.object.event.ObjectChangeAction.UNSPECIFIED;
import static com.dbn.object.event.ObjectChangeAction.UPDATE;

@Slf4j
class DBObjectBundleMonitor implements ObjectChangeListener {
    private final WeakRef<DBObjectBundle> objectBundle;

    public DBObjectBundleMonitor(DBObjectBundle objectBundle) {
        this.objectBundle = WeakRef.of(objectBundle);
    }

    @Override
    public void objectsChanged(ObjectChangeEvent event) {
        ConnectionId connectionId = getConnectionId();
        if (!event.matches(connectionId)) return;

        DBObjectBundle objectBundle = getObjectBundle();
        String connectionName = objectBundle.getConnection().getName();

        DBObject object = event.getObject();
        ObjectChangeAction action = event.getChangeAction();
        if (object != null && !event.isRootObjectEvent() && action.isOneOf(UPDATE, REFRESH, ENABLE, DISABLE, LOCK, UNLOCK)) {
            log.info("{}: refreshing {}", connectionName, object.getQualifiedNameWithType());
            object.refresh();
            return;
        }

        DBObjectType objectType = event.getObjectType();
        SchemaId ownerId = event.getOwnerId();

        if (event.isRootObjectEvent()) {
            log.info("{}: refreshing root objects of type {}", connectionName, objectType);
            refreshRootObjects(objectType, action);

        } else if (event.isSchemaObjectEvent()) {
            log.info("{}: refreshing schema objects of type {} owned by {}", connectionName, objectType, ownerId);
            refreshSchemaObjects(ownerId, objectType, action);

        } else if (event.isChildObjectEvent()) {
            DBObject parent = nn(object).getParentObject();
            log.info("{}: refreshing child objects of type {} in {}", connectionName, objectType, parent.getQualifiedNameWithType());
            refreshChildObjects(parent, objectType, action);
        }
    }

    private void refreshChildObjects(DBObject parent, DBObjectType objectType, ObjectChangeAction action) {
        SchemaId schemaId = parent.getSchemaId();
        if (schemaId != null) {
            refreshSchemaObjects(schemaId, objectType, action);
        }

        if (action.isOneOf(CREATE, DELETE, RENAME, UNSPECIFIED)) {
            DBObjectList<DBObject> objectList = parent.getChildObjectList(objectType);
            if (objectList == null) return;

            markDirty(objectList);
        }
    }

    private void refreshRootObjects(DBObjectType objectType, ObjectChangeAction action) {
        // Reload the root list to update the changed object's own metadata (e.g. lastUpdated).
        if (action.isOneOf(CREATE, UPDATE, DELETE, RENAME, LOCK, UNLOCK, ENABLE, DISABLE, UNSPECIFIED)) {
            DBObjectBundle objectBundle = getObjectBundle();
            DBObjectListContainer objectLists = objectBundle.getObjectLists();
            DBObjectList<DBObject> objectList = objectLists.getObjectList(objectType);
            markDirty(objectList);

            if (objectType == DBObjectType.USER && action.isOneOf(CREATE, DELETE, RENAME, UNSPECIFIED)) {
                markDirty(objectLists.getObjectList(DBObjectType.SCHEMA));
            }
        }
    }

    private void refreshSchemaObjects(SchemaId ownerId, DBObjectType objectType, ObjectChangeAction action) {
        DBObjectBundle objectBundle = getObjectBundle();
        DBSchema schema = objectBundle.getSchema(ownerId.id());
        if (schema == null) return;

        if (action.isOneOf(CREATE, DELETE, RENAME, UNSPECIFIED)) {
            refreshSchemaObjects(schema, objectType);
        }

        if (action == RENAME && objectType == DBObjectType.MINING_MODEL) {
            refreshSchemaObjects(schema, DBObjectType.VIEW);
        }

        if (action == RENAME && objectType.isOneOf(DBObjectType.COLUMN, DBObjectType.CONSTRAINT, DBObjectType.INDEX)) {
            refreshSchemaObjects(schema, DBObjectType.COLUMN);
            refreshSchemaObjects(schema, DBObjectType.CONSTRAINT);
            refreshSchemaObjects(schema, DBObjectType.INDEX);
        }

        if (action.isOneOf(CREATE, UPDATE, DELETE, RENAME, UNSPECIFIED)) {
            for (DBObjectType childObjectType : objectType.getTreeChildren()) {
                refreshSchemaObjects(schema, childObjectType);
            }
        }
    }

    private static void refreshSchemaObjects(DBSchema schema, DBObjectType objectType) {
        DBObjectList<DBObject> objectList = schema.getChildObjectList(objectType);
        if (objectList != null) {
            markDirty(objectList);
            return;
        }

        objectType.getInheritingTypes()
                .stream()
                .map(t -> schema.getChildObjectList(t))
                .forEach(l -> markDirty(l));
    }

    private static void markDirty(DBObjectList<DBObject> objectList) {
        if (objectList == null) return;
        if (!objectList.isLoaded()) return;

        objectList.markDirty();
    }

    public DBObjectBundle getObjectBundle() {
        return objectBundle.ensure();
    }

    private ConnectionId getConnectionId() {
        return getObjectBundle().getConnectionId();
    }


}
