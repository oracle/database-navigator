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


import com.dbn.execution.java.wrapper.WrapperBuilder.ComplexTypeKey;
import lombok.Getter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Holds all per-invocation data structures used by {@link WrapperBuilder}.
 */
@Getter
public class WrapperBuilderContext {

    //methods for complexTypeMap
    private final Map<ComplexTypeKey, JavaComplexType> complexTypeMap;
    //methods for complexTypeSet
    private final Set<ComplexTypeKey> complexTypeSet;

    /**
     * Instantiates a fresh context for each parse invocation.
     */
    public WrapperBuilderContext() {
        this.complexTypeMap = new HashMap<>();
        this.complexTypeSet = new HashSet<>();
    }


    public void addMapEntry(ComplexTypeKey key, JavaComplexType javaComplexType){
        complexTypeMap.put(key, javaComplexType);
    }

    public JavaComplexType getJavaComplexType(ComplexTypeKey key){
        return complexTypeMap.get(key);
    }


    public boolean detectRepetition(ComplexTypeKey key)
    {
        return complexTypeSet.contains(key);
    }

    public void addToSet(ComplexTypeKey key){
        complexTypeSet.add(key);
    }

    public void removeFromSet(ComplexTypeKey key)
    {
        complexTypeSet.remove(key);
    }
}

