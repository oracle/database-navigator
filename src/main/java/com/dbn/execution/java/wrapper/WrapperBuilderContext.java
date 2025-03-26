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
import com.dbn.execution.java.wrapper.WrapperBuilder.WrapperMethodKey;
import com.dbn.execution.java.wrapper.model.ClassWrapper;
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

    private final Map<ComplexTypeKey, ClassWrapper> classWrappers;
    private final Set<ComplexTypeKey> complexTypeSet;
    private final Map<WrapperMethodKey, Integer> wrapperMethodNames;
    private final Map<ComplexTypeKey, Integer> complexTypeIndexes;

    /**
     * Instantiates a fresh context for each parse invocation.
     */
    public WrapperBuilderContext() {
        this.classWrappers = new HashMap<>();
        this.complexTypeSet = new HashSet<>();
        this.wrapperMethodNames = new HashMap<>();
        this.complexTypeIndexes = new HashMap<>();
    }


    public void addClassWrapper(ComplexTypeKey key, ClassWrapper classWrapper){
        classWrappers.put(key, classWrapper);
    }

    public ClassWrapper getClassWrapper(ComplexTypeKey key){
        return classWrappers.get(key);
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

    public void addToIndex(ComplexTypeKey key, int index){complexTypeIndexes.put(key, index);}

    public int getComplexTypeIndex(ComplexTypeKey key){return complexTypeIndexes.get(key);}

    public int getComplexTypeIndex(String className, short arrayLength){
        ComplexTypeKey key = new ComplexTypeKey(className, arrayLength);
        return complexTypeIndexes.get(key);
    }

    public String getAndAddWrapperMethodName(String originalMethodName, String methodSignature)
    {
        WrapperMethodKey key = new WrapperMethodKey(originalMethodName, methodSignature);
        return getAndAddWrapperMethodName(key);
    }

    public String getAndAddWrapperMethodName(WrapperMethodKey key) {
        if (wrapperMethodNames.containsKey(key)) {
            int count = wrapperMethodNames.get(key) + 1;
            wrapperMethodNames.put(key, count);
            return key.getOriginalMethodName() +"_"+ count;
        } else {
            wrapperMethodNames.put(key, 0);
            return key.getOriginalMethodName();
        }
    }

}

