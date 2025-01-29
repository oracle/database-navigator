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

package com.dbn.object.common.list.loader;

import com.dbn.common.content.DynamicContent;
import com.dbn.common.content.DynamicContentElement;
import com.dbn.common.content.DynamicContentType;
import com.dbn.common.content.loader.DynamicSubcontentCustomLoader;
import com.dbn.common.util.Commons;
import com.dbn.database.common.metadata.DBObjectMetadata;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.common.list.DBObjectRelation;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DBObjectListFromRelationListLoader<
                T extends DynamicContentElement,
                M extends DBObjectMetadata>
        extends DynamicSubcontentCustomLoader<T, M> {

    private DBObjectListFromRelationListLoader(String identifier, @Nullable DynamicContentType parentContentType, @NotNull DynamicContentType contentType) {
        super(identifier, parentContentType, contentType);
    }

    public static <T extends DynamicContentElement, M extends DBObjectMetadata> DBObjectListFromRelationListLoader<T, M> create(
            @NonNls String identifier,
            @Nullable DynamicContentType parentContentType,
            @NotNull DynamicContentType contentType) {
        return new DBObjectListFromRelationListLoader<>(identifier, parentContentType, contentType);
    }

    @Override
    public T resolveElement(DynamicContent<T> dynamicContent, DynamicContentElement sourceElement) {
        DBObjectList objectList = (DBObjectList) dynamicContent;
        DBObjectRelation objectRelation = (DBObjectRelation) sourceElement;
        DBObject object = (DBObject) objectList.getParent();

        if (Commons.match(object, objectRelation.getSourceObject())) {
            return (T) objectRelation.getTargetObject();
        }
        if (Commons.match(object, objectRelation.getTargetObject())) {
            return (T) objectRelation.getSourceObject();
        }

        return null;
    }
}
