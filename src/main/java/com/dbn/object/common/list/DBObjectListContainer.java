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

package com.dbn.object.common.list;

import com.dbn.common.Direction;
import com.dbn.common.content.DynamicContentProperty;
import com.dbn.common.content.DynamicContentType;
import com.dbn.common.content.dependency.BasicDependencyAdapter;
import com.dbn.common.content.dependency.ContentDependencyAdapter;
import com.dbn.common.content.dependency.DualContentDependencyAdapter;
import com.dbn.common.content.dependency.SubcontentDependencyAdapter;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.dispose.UnlistedDisposable;
import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.util.Commons;
import com.dbn.connection.DatabaseEntity;
import com.dbn.connection.DatabaseType;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.object.type.DBObjectRelationType;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.dbn.common.Direction.DOWN;
import static com.dbn.common.Direction.UP;
import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.dispose.Failsafe.guarded;
import static com.dbn.common.util.Unsafe.cast;
import static java.util.Collections.emptyList;

@Getter
public final class DBObjectListContainer implements StatefulDisposable, UnlistedDisposable {
    private static final DynamicContentTypeIndex<DBObjectType, DBObjectType> OBJECT_INDEX = new DynamicContentTypeIndex<>(DBObjectType.class);
    private static final DynamicContentTypeIndex<DBObjectType, DBObjectRelationType> RELATION_INDEX = new DynamicContentTypeIndex<>(DBObjectType.class);

    private static final DBObjectList<?>[] DISPOSED_OBJECTS = new DBObjectList[0];
    private static final DBObjectRelationList[] DISPOSED_RELATIONS = new DBObjectRelationList[0];

    private DatabaseEntity owner;
    private DBObjectList<?>[] objects;
    private DBObjectRelationList[] relations;

    public DBObjectListContainer(@NotNull DatabaseEntity owner) {
        this.owner = owner;
    }

    @NotNull
    private DatabaseEntity getOwner() {
        return Failsafe.nn(owner);
    }

    public void visit(DBObjectListVisitor visitor, boolean visitInternal) {
        if (objects == null) return;
        guarded(this, o -> {
            if (isNotValid(visitor)) return;
            for (DBObjectList<?> list : o.objects) {
                if (list == null) continue;
                if (list.isInternal() && !visitInternal) continue;
                if (isNotValid(list)) continue;
                if (isNotValid(visitor)) return;

                ProgressMonitor.checkCancelled();
                visitor.visit(list);
            }
        });
    }
    @NotNull
    public <T extends DBObject> List<T> getObjects(DBObjectType objectType) {
        DBObjectList<T> objects = getObjectList(objectType);
        return objects == null ? emptyList() : objects.getObjects();
    }

    private DBObjectType getOwnerType() {
        DatabaseEntity owner = getOwner();
        if (owner instanceof DBObject) {
            DBObject object = (DBObject) owner;
            return object.getObjectType();
        } else if (owner instanceof DBObjectBundle) {
            return DBObjectType.BUNDLE;
        }

        throw new IllegalArgumentException();
    }

    private DatabaseType getDatabaseType() {
        return getOwner().getConnection().getDatabaseType();
    }

    @Nullable
    public <T extends DBObject> DBObjectList<T> getObjectList(DBObjectType objectType) {
        DBObjectList<T> objectList = objects(objectType);
        if (isValid(objectList)) return objectList;

        Set<DBObjectType> inheritingTypes = objectType.getInheritingTypes();
        if (inheritingTypes.isEmpty()) return null;

        for (DBObjectType inheritingType : inheritingTypes) {
            objectList = objects(inheritingType);
            if (isValid(objectList)) return objectList;
        }

        return null;
    }

    public <T extends DBObject> T getObject(DBObjectType objectType, String name, short overload) {
        return objectType == DBObjectType.ANY  ?
                findAnyObject(name, overload) :
                findObject(objectType, name, overload, Direction.ANY);
    }

    @Nullable
    private <T extends DBObject> T findAnyObject(String name, short overload) {
        for (DBObjectList<?> objectList : getObjects()) {
            if (objectList == null) continue;
            if (objectList.isDependency()) continue;
            if (objectList.isHidden()) continue;

            DBObject object = objectList.getObject(name, overload);
            if (object != null) return cast(object);
        }
        return null;
    }

    @Nullable
    private <T extends DBObject> T findObject(DBObjectType objectType, String name, short overload, Direction direction) {
        DBObjectList<?> objectList = getObjectList(objectType);

        if (objectList != null && !objectList.isHidden()) {
            return cast(objectList.getObject(name, overload));
        }

        switch (direction) {
            case UP:   return findInheritedObject(objectType, name, overload);
            case DOWN: return findInheritingObject(objectType, name, overload);
            case ANY:  return Commons.coalesce(
                        () -> findInheritedObject(objectType, name, overload),
                        () -> findInheritingObject(objectType, name, overload));
        }
        return null;
    }

    @Nullable
    private <T extends DBObject> T findInheritedObject(DBObjectType objectType, String name, short overload) {
        DBObjectType inheritedType = objectType.getInheritedType();
        if (inheritedType != null && inheritedType != objectType) {
            return findObject(inheritedType, name, overload, UP);
        }
        return null;
    }

    @Nullable
    private <T extends DBObject> T findInheritingObject(DBObjectType objectType, String name, short overload) {
        Set<DBObjectType> inheritingTypes = objectType.getInheritingTypes();
        if (!inheritingTypes.isEmpty()) {
            for (DBObjectType objType : inheritingTypes) {
                DBObject object = findObject(objType, name, overload, DOWN);
                if (object != null) {
                    return cast(object);
                }
            }
        }
        return null;
    }

    @Nullable
    public <T extends DBObject> T getObjectForParentType(DBObjectType parentObjectType, String name, short overload) {
        if (objects == null) return null;

        for (DBObjectList<?> objectList : objects) {
            if (isNotValid(objectList) || objectList.isHidden() || objectList.isDependency()) continue;

            DBObjectType objectType = objectList.getObjectType();
            if (!parentObjectType.isParentOf(objectType)) continue;

            DBObject object = objectList.getObject(name, overload);
            if (isNotValid(object)) continue;

            return cast(object);
        }

        return null;
    }

    private boolean isSupported(DBObjectType objectType) {
        DatabaseEntity owner = getOwner();
        return objectType.isSupported(owner);
    }

    public <T extends DBObject> T getObjectNoLoad(String name, short overload) {
        if (objects == null) return null;

        for (DBObjectList<?> objectList : objects) {
            if (isNotValid(objectList) || !objectList.isLoaded() || objectList.isDirty())  continue;

            DBObject object = objectList.getObject(name, overload);
            if (isNotValid(object)) continue;

            DatabaseEntity owner = getOwner();
            if (owner.isObjectBundle()) {
                return cast(object);
            }

            if (owner.isObject()) {
                DBObject ownerObject = (DBObject) owner;
                if (ownerObject.isParentOf(object)) {
                    return cast(object);
                }
            }
        }
        return null;

    }


    /*************************************************************
     *             Object Lists -  factory utilities             *
     *************************************************************/
    @Nullable
    public <T extends DBObject> DBObjectList<T>  createObjectList(
            @NotNull DBObjectType objectType,
            @NotNull DatabaseEntity treeParent,
            DynamicContentProperty... properties) {

        if (!isSupported(objectType)) return null;
        BasicDependencyAdapter dependencyAdapter = objectType == DBObjectType.CONSOLE ?
                BasicDependencyAdapter.FAST :
                BasicDependencyAdapter.REGULAR;
        return createObjectList(objectType, treeParent, dependencyAdapter, properties);
    }

    @Nullable
    public <T extends DBObject> DBObjectList<T> createSubcontentObjectList(
            @NotNull DBObjectType objectType,
            @NotNull DatabaseEntity treeParent,
            DatabaseEntity sourceContentHolder,
            DynamicContentType<?> sourceContentType,
            DynamicContentProperty... properties) {

        if (!isSupported(objectType) || sourceContentHolder == null) return null;

        val dynamicContent = sourceContentHolder.getDynamicContent(sourceContentType);
        if (dynamicContent == null) return null;

        ContentDependencyAdapter dependencyAdapter = SubcontentDependencyAdapter.create(sourceContentHolder, sourceContentType);
        return createObjectList(objectType, treeParent, dependencyAdapter, properties);
    }

    @Nullable
    public <T extends DBObject> DBObjectList<T> createSubcontentObjectList(
            @NotNull DBObjectType objectType,
            @NotNull DatabaseEntity treeParent,
            DBObject sourceContentHolder,
            DynamicContentProperty... properties) {

        if (!isSupported(objectType)) return null;

        val dynamicContent = sourceContentHolder.getDynamicContent(objectType);
        if (dynamicContent == null) return null;

        ContentDependencyAdapter dependencyAdapter = SubcontentDependencyAdapter.create(sourceContentHolder, objectType);
        return createObjectList(objectType, treeParent, dependencyAdapter, properties);
    }

    private <T extends DBObject> DBObjectList<T> createObjectList(
            @NotNull DBObjectType objectType,
            @NotNull DatabaseEntity parent,
            ContentDependencyAdapter dependencyAdapter,
            DynamicContentProperty... properties) {

        boolean grouped = Commons.isOneOf(DynamicContentProperty.GROUPED, properties);
        DBObjectList<T> objectList = grouped ?
                new DBObjectListImpl.Grouped<>(objectType, parent, dependencyAdapter, properties):
                new DBObjectListImpl<>(objectType, parent, dependencyAdapter, properties);
        addObjectList(objectList);

        return objectList;
    }

    public void addObjectList(DBObjectList<?> objectList) {
        if (objectList != null) {
            addObjects(objectList);
        }
    }

    public void loadObjects() {
        visit(o -> o.load(), false);
    }

    public void reloadObjects() {
        visit(o -> o.reload(), true);
    }

    public void refreshObjects() {
        visit(o -> o.refresh(), true);
    }

    public void loadObjects(DBObjectType objectType) {
        DBObjectList<?> objectList = getObjectList(objectType);
        if (objectList != null) objectList.getElements();
    }

    /*****************************************************************
     *                      Object Relation Lists                    *
     *****************************************************************/
    private boolean isSupported(DBObjectRelationType relationType) {
        return isSupported(relationType.getSourceType()) &&
                isSupported(relationType.getTargetType());
    }

    @Nullable
    public <T extends DBObjectRelation> DBObjectRelationList<T> getRelations(DBObjectRelationType relationType) {
        if (relations == null) return null;
        for (DBObjectRelationList objectRelations : relations) {
            if (objectRelations.getRelationType() == relationType) {
                return cast(objectRelations);
            }
        }
        return null;
    }

    public void createObjectRelationList(
            DBObjectRelationType type,
            DatabaseEntity parent,
            DBObjectList firstContent,
            DBObjectList secondContent,
            DynamicContentProperty... properties) {

        if (!isSupported(type)) return;
        ContentDependencyAdapter dependencyAdapter = DualContentDependencyAdapter.create(firstContent, secondContent);
        createObjectRelationList(type, parent, dependencyAdapter, properties);
    }

    public void createSubcontentObjectRelationList(
            DBObjectRelationType relationType,
            DatabaseEntity parent,
            DBObject sourceContentObject,
            DynamicContentProperty... properties) {

        if (!isSupported(relationType)) return;
        ContentDependencyAdapter dependencyAdapter = SubcontentDependencyAdapter.create(sourceContentObject, relationType);
        createObjectRelationList(relationType, parent, dependencyAdapter, properties);
    }


    private void createObjectRelationList(
            DBObjectRelationType type,
            DatabaseEntity parent,
            ContentDependencyAdapter dependencyAdapter,
            DynamicContentProperty... properties) {

        if (!isSupported(type)) return;
        boolean grouped = Commons.isOneOf(DynamicContentProperty.GROUPED, properties);
        DBObjectRelationList relations = grouped ?
                new DBObjectRelationListImpl.Grouped(type, parent, dependencyAdapter, properties) :
                new DBObjectRelationListImpl(type, parent, dependencyAdapter, properties);
        addRelations(relations);
    }

    private void addObjects(DBObjectList objects) {
        if (objects == null) return;

        int index = objectsIndex(objects.getObjectType());
        int length = index + 1;

        if (this.objects == null)
            this.objects = new DBObjectList[length]; else
            this.objects = Arrays.copyOf(this.objects, length);

        this.objects[index] = objects;
    }

    private void addRelations(DBObjectRelationList relations) {
        int index = relationsIndex(relations.getRelationType());
        int length = index + 1;

        if (this.relations == null)
            this.relations = new DBObjectRelationList[length]; else
            this.relations = Arrays.copyOf(this.relations, length);

        this.relations[index] = relations;
    }

    private int objectsIndex(DBObjectType objectType) {
        return OBJECT_INDEX.index(
                getDatabaseType(),
                getOwnerType(),
                objectType);
    }

    private int relationsIndex(DBObjectRelationType relationType) {
        return RELATION_INDEX.index(
                getDatabaseType(),
                getOwnerType(),
                relationType);
    }

    @Nullable
    private <T extends DBObject> DBObjectList<T> objects(DBObjectType objectType) {
        if (objects != null && objects != DISPOSED_OBJECTS) {
            int index = objectsIndex(objectType);
            if (index < objects.length) {
                return cast(objects[index]);
            }
        }

        return null;
    }

    /*****************************************************************
     *                      Disposable
     *****************************************************************/


    @Override
    public boolean isDisposed() {
        return objects == DISPOSED_OBJECTS || relations == DISPOSED_RELATIONS;
    }

    @Override
    public void setDisposed(boolean disposed) {
        //
    }

    @Override
    public void disposeInner() {
        this.objects = Disposer.replace(this.objects, DISPOSED_OBJECTS);
        this.relations = Disposer.replace(this.relations, DISPOSED_RELATIONS);
        this.owner = null;
    }
}
