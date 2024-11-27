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

import com.dbn.common.content.DynamicContentBase;
import com.dbn.common.content.DynamicContentProperty;
import com.dbn.common.content.DynamicContentType;
import com.dbn.common.content.GroupedDynamicContent;
import com.dbn.common.content.dependency.ContentDependencyAdapter;
import com.dbn.common.content.loader.DynamicContentLoader;
import com.dbn.common.content.loader.DynamicContentLoaderImpl;
import com.dbn.common.range.Range;
import com.dbn.common.util.Commons;
import com.dbn.connection.DatabaseEntity;
import com.dbn.database.common.metadata.DBObjectMetadata;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectRelationType;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.dbn.common.list.FilteredList.unwrap;
import static java.util.Collections.emptyList;

@Slf4j
@Getter
class DBObjectRelationListImpl<T extends DBObjectRelation> extends DynamicContentBase<T> implements DBObjectRelationList<T>{
    private final DBObjectRelationType relationType;

    public DBObjectRelationListImpl(
            @NotNull DBObjectRelationType type,
            @NotNull DatabaseEntity parent,
            ContentDependencyAdapter dependencyAdapter,
            DynamicContentProperty... properties) {
        super(parent, dependencyAdapter, properties);
        this.relationType = type;
    }

    @Override
    public DynamicContentLoader<T, DBObjectMetadata> getLoader() {
        DynamicContentType<?> parentContentType = getParentEntity().getDynamicContentType();
        return DynamicContentLoaderImpl.resolve(parentContentType, relationType);
    }

    @Override
    @NotNull
    public List<T> getObjectRelations() {
        return getAllElements();
    }

    @Override
    public DynamicContentType getContentType() {
        return relationType;
    }

    @NotNull
    @Override
    public String getName() {
        return
                relationType.getSourceType().getName() + " " +
                relationType.getTargetType().getListName();
    }

    public String toString() {
        return relationType + " - " + super.toString();
    }

    @Override
    public List<DBObjectRelation> getRelationBySourceName(String sourceName) {
        List<DBObjectRelation> objectRelations = new ArrayList<>();
        for (DBObjectRelation objectRelation : getAllElements()) {
            if (Objects.equals(objectRelation.getSourceObject().getName(), sourceName)) {
                objectRelations.add(objectRelation);
            }
        }
        return objectRelations;
    }

    @Override
    public List<DBObjectRelation> getRelationByTargetName(String targetName) {
        List<DBObjectRelation> objectRelations = new ArrayList<>();
        for (DBObjectRelation objectRelation : getAllElements()) {
            if (Objects.equals(objectRelation.getTargetObject().getName(), targetName)) {
                objectRelations.add(objectRelation);
            }
        }
        return objectRelations;
    }

    @Override
    protected void sortElements(List<T> elements) {
        elements.sort(null);
    }

    /*********************************************************
     *                   DynamicContent                      *
     *********************************************************/

    @NotNull
    @Override
    public Project getProject() {
        return getParentEntity().getProject();
    }

   @Override
   public String getContentDescription() {
        if (getParentEntity() instanceof DBObject) {
            DBObject object = getParentEntity();
            return getName() + " of " + object.getQualifiedNameWithType();
        }
       return getName() + " from " + this.getConnection().getName() ;
    }

    @Override
    public void notifyChangeListeners() {}

    public static class Grouped<T extends DBObjectRelation> extends DBObjectRelationListImpl<T> implements GroupedDynamicContent<T> {
        private Map<DBObjectRef, Range> ranges;

        Grouped(
                @NotNull DBObjectRelationType relationType,
                @NotNull DatabaseEntity parent,
                ContentDependencyAdapter dependencyAdapter,
                DynamicContentProperty... properties) {
            super(relationType, parent, dependencyAdapter, properties);
            set(DynamicContentProperty.GROUPED, true);
        }


        @Override
        protected void afterUpdate() {
            List<T> elements = unwrap(this.elements);
            if (elements.isEmpty()) return;

            Map<DBObjectRef, Range> ranges = new HashMap<>();

            DBObjectRef currentObject = null;
            int rangeStart = 0;
            for (int i = 0; i < elements.size(); i++) {
                T objectRelation = elements.get(i);
                DBObject sourceObject = objectRelation.getSourceObject();
                DBObject object = Commons.nvl(sourceObject.getParentObject(), sourceObject);
                currentObject = Commons.nvl(currentObject, object.ref());

                if (!Objects.equals(currentObject, object.ref())) {
                    ranges.put(currentObject, new Range(rangeStart, i - 1));
                    currentObject = object.ref();
                    rangeStart = i;
                }

                if (i == elements.size() - 1) {
                    ranges.put(currentObject, new Range(rangeStart, i));
                }
            }

            this.ranges = ranges;
        }

        public List<T> getChildElements(DatabaseEntity entity) {
            List<T> elements = getAllElements();
            val ranges = this.ranges;
            if (ranges == null) return emptyList();
            if(!entity.isObject()) return emptyList();

            DBObject object = (DBObject) entity;
            Range range = ranges.get(object.ref());
            if (range == null) return emptyList();

            int size = elements.size();
            if (size == 0) return emptyList();

            int fromIndex = range.getLeft();
            int toIndex = range.getRight() + 1;
            if (toIndex > size) {
                log.error("invalid range {} for elements size {}", range, elements.size(),
                        new IllegalArgumentException("Invalid range capture"));
                toIndex = size;
            }

            return elements.subList(fromIndex, toIndex);
        }

        @Override
        public T getElement(String name, short overload) {
    /*        if (parentNameRanges != null) {
                for (Range range : parentNameRanges.values()) {
                    SearchAdapter<T> binary = getObjectType().isOverloadable() ?
                            binary(name, overload) :
                            binary(name);

                    T element = Search.binarySearch(elements, range.getLeft(), range.getRight(), binary);
                    if (element != null) {
                        return element;
                    }
                }
            }*/
            return null;
        }
    }
}
