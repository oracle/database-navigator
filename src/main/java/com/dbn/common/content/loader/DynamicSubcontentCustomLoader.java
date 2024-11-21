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

package com.dbn.common.content.loader;

import com.dbn.common.content.DynamicContent;
import com.dbn.common.content.DynamicContentElement;
import com.dbn.common.content.DynamicContentProperty;
import com.dbn.common.content.DynamicContentType;
import com.dbn.common.content.GroupedDynamicContent;
import com.dbn.common.content.dependency.ContentDependencyAdapter;
import com.dbn.common.content.dependency.SubcontentDependencyAdapter;
import com.dbn.connection.DatabaseEntity;
import com.dbn.database.common.metadata.DBObjectMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class DynamicSubcontentCustomLoader<
                T extends DynamicContentElement,
                M extends DBObjectMetadata>
        extends DynamicContentLoaderImpl<T, M>
        implements DynamicContentLoader<T, M> {

    public DynamicSubcontentCustomLoader(
            String identifier, @Nullable DynamicContentType parentContentType,
            @NotNull DynamicContentType contentType) {

        super(identifier, parentContentType, contentType, true);
    }

    protected abstract T resolveElement(DynamicContent<T> dynamicContent, DynamicContentElement sourceElement);

    @Override
    public void loadContent(DynamicContent<T> content) {
        List<T> list = null;
        ContentDependencyAdapter adapter = content.getDependencyAdapter();
        if (adapter instanceof SubcontentDependencyAdapter) {
            SubcontentDependencyAdapter dependencyAdapter = (SubcontentDependencyAdapter) adapter;
            DynamicContent sourceContent = dependencyAdapter.getSourceContent();
            if (sourceContent instanceof GroupedDynamicContent) {
                GroupedDynamicContent groupedContent = (GroupedDynamicContent) sourceContent;
                DatabaseEntity parentEntity = content.ensureParentEntity();
                List<DynamicContentElement> childElements = groupedContent.getChildElements(parentEntity);
                list = childElements.stream().map(e -> resolveElement(content, e)).filter(e -> e != null).collect(Collectors.toList());
            } else {
                List elements = sourceContent.getAllElements();
                for (Object object : elements) {
                    content.checkDisposed();
                    DynamicContentElement sourceElement = (DynamicContentElement) object;
                    T element = resolveElement(content, sourceElement);
                    if (element != null) {
                        content.checkDisposed();
                        if (list == null) {
                            list = new ArrayList<>();
                        }
                        list.add(element);
                    }
                }

            }
        }

        content.setElements(list);
        content.set(DynamicContentProperty.MASTER, false);
    }
}
