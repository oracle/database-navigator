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

package com.dbn.execution.java.wrapper;


import com.dbn.common.Pair;
import com.dbn.execution.java.wrapper.model.ClassWrapper;
import com.dbn.execution.java.wrapper.naming.FriendlyWrapperNamingProvider;
import com.dbn.execution.java.wrapper.naming.TransientWrapperNamingProvider;
import com.dbn.execution.java.wrapper.naming.WrapperNamingProvider;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds all per-invocation data structures used by {@link WrapperModelBuilder}.
 */
@Getter
@Setter
public class WrapperContext {
    private final WrapperModelInput input;
    private final WrapperNamingProvider namingProvider;
    private final Map<Pair<String, Integer>, ClassWrapper> classWrapperCache = new HashMap<>();

    private WrapperModel model;

    /**
     * Instantiates a fresh context for each parse invocation.
     */
    public WrapperContext(WrapperModelInput input) {
        this.input = input;
        this.namingProvider = input.isUseFriendlyNames() ?
                new FriendlyWrapperNamingProvider():
                new TransientWrapperNamingProvider();
    }


    public void cacheClassWrapper(ClassWrapper classWrapper) {
        var key = Pair.of(classWrapper.getClassName(), classWrapper.getArrayDepth());
        classWrapperCache.put(key, classWrapper);
    }

    @Nullable
    public ClassWrapper getCachedClassWrapper(String className, int arrayDepth) {
        var key = Pair.of(className, arrayDepth);
        return classWrapperCache.get(key);
    }

}

