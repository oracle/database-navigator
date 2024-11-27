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
import com.dbn.common.content.DynamicContentType;
import com.dbn.common.routine.ParametricRunnable;
import com.dbn.common.util.Commons;
import com.dbn.database.common.metadata.DBObjectMetadata;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.content.DynamicContentType.NULL;

@Slf4j
@Getter
public abstract class DynamicContentLoaderImpl<
                T extends DynamicContentElement,
                M extends DBObjectMetadata>
        implements DynamicContentLoader<T, M>{

    private static final Map<DynamicContentType, Map<DynamicContentType, DynamicContentLoader>> LOADERS = new ConcurrentHashMap<>();
    private final String identifier;

    DynamicContentLoaderImpl(String identifier, @Nullable DynamicContentType parentContentType, @NotNull DynamicContentType contentType, boolean register) {
        this.identifier = identifier;
        if (register) register(parentContentType, contentType, this);
    }

    public static <T extends DynamicContentElement, M extends DBObjectMetadata> DynamicContentLoader<T, M> create(
            String identifier,
            @Nullable DynamicContentType parentContentType,
            @NotNull DynamicContentType contentType,
            boolean register,
            ParametricRunnable<DynamicContent<T>, SQLException> loader) {
        return new DynamicContentLoaderImpl<>(identifier, parentContentType, contentType, register) {
            @Override
            public void loadContent(DynamicContent<T> content) throws SQLException {
                loader.run(content);
            }
        };
    }

    private static void register(
            @Nullable DynamicContentType parentContentType,
            @NotNull DynamicContentType contentType,
            @NotNull DynamicContentLoader loader) {

        parentContentType = Commons.nvl(parentContentType, NULL);
        Map<DynamicContentType, DynamicContentLoader> childLoaders = LOADERS.computeIfAbsent(parentContentType, t -> new HashMap<>());
        DynamicContentLoader contentLoader = childLoaders.get(contentType);
        if (contentLoader == null) {
            childLoaders.put(contentType, loader);
        } else if (contentLoader != loader){
            log.error("Duplicate content loader registration for parentContentType={} and contentType={}", parentContentType, contentType);
        }
    }

    @NotNull
    public static <T extends DynamicContentElement, M extends DBObjectMetadata> DynamicContentLoader<T, M> resolve(
            @Nullable DynamicContentType<?> parentContentType,
            @NotNull DynamicContentType<?> contentType) {

        DynamicContentType<?> lookupParentContentType = Commons.nvl(parentContentType, NULL);
        while (lookupParentContentType != null) {
            Map<DynamicContentType, DynamicContentLoader> loaderMap = LOADERS.get(lookupParentContentType);
            if (loaderMap != null) {
                DynamicContentType<?> lookupContentType = contentType;
                while (lookupContentType != null) {
                    DynamicContentLoader loader = loaderMap.get(lookupContentType);
                    if (loader != null) {
                        return loader;
                    }
                    DynamicContentType<?> genericContentType = lookupContentType.getGenericType();
                    lookupContentType = genericContentType == lookupContentType ? null : genericContentType;
                }
            }
            DynamicContentType<?> genericParentContentType = lookupParentContentType.getGenericType();
            lookupParentContentType =
                    genericParentContentType == NULL ? null :
                    genericParentContentType == lookupParentContentType ? NULL :
                    genericParentContentType;
        }

        throw new UnsupportedOperationException("No entry found for content type "+ lookupParentContentType + " / " + contentType);
    }
}
