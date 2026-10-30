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

package com.dbn.common.action;

import com.dbn.options.general.WorkspaceFeature;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** Resolves feature tags without retaining action instances or their class loaders in a global map. */
@UtilityClass
public class ActionFeatures {
    private static final ClassValue<Optional<WorkspaceFeature>> FEATURES = new ClassValue<>() {
        @Override
        protected Optional<WorkspaceFeature> computeValue(@NotNull Class<?> type) {
            return Optional.ofNullable(findFeature(type));
        }
    };

    public static @Nullable WorkspaceFeature getFeature(@NotNull Class<?> actionClass) {
        return FEATURES.get(actionClass).orElse(null);
    }

    /**
     * At each class layer, an explicit tag wins over its support interfaces, then its superclass.
     * Interface hierarchies follow declaration order. A concrete class tag overrides all inherited tags.
     */
    private static @Nullable WorkspaceFeature findFeature(@Nullable Class<?> type) {
        if (type == null || type == Object.class) return null;

        ActionFeature annotation = type.getDeclaredAnnotation(ActionFeature.class);
        if (annotation != null) return annotation.value();

        for (Class<?> support : type.getInterfaces()) {
            WorkspaceFeature feature = findFeature(support);
            if (feature != null) return feature;
        }
        return findFeature(type.getSuperclass());
    }
}
