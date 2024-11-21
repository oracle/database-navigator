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

package com.dbn.common.environment;

import com.dbn.common.util.Cloneable;
import com.dbn.common.util.CollectionUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
public class EnvironmentTypeBundle implements Iterable<EnvironmentType>, Cloneable {
    private final List<EnvironmentType> environmentTypes = new ArrayList<>();
    public static final EnvironmentTypeBundle DEFAULT = new EnvironmentTypeBundle();

    private EnvironmentTypeBundle() {
        List<EnvironmentType> environmentTypes = Arrays.asList(
                EnvironmentType.DEVELOPMENT,
                EnvironmentType.INTEGRATION,
                EnvironmentType.PRODUCTION,
                EnvironmentType.OTHER);
        setElements(environmentTypes);
    }
    
    public EnvironmentTypeBundle(EnvironmentTypeBundle source) {
        setElements(source.environmentTypes);
    }  
    
    private void setElements(List<EnvironmentType> environmentTypes) {
        this.environmentTypes.clear();
        CollectionUtil.cloneElements(environmentTypes, this.environmentTypes);
    }
    
    @NotNull
    public EnvironmentType getEnvironmentType(EnvironmentTypeId id) {
        for (EnvironmentType environmentType : this) {
            if (environmentType.getId() == id) {
                return environmentType;
            }
        }
        return EnvironmentType.DEFAULT;
    }

    @Override
    public Iterator<EnvironmentType> iterator() {
        return environmentTypes.iterator();
    }

    public void clear() {
        environmentTypes.clear();
    }

    public void add(EnvironmentType environmentType) {
        environmentTypes.add(environmentType);
    }
    
    public void add(int index, EnvironmentType environmentType) {
        environmentTypes.add(index, environmentType);
    }
    

    public int size() {
        return environmentTypes.size();
    }
    
    public EnvironmentType get(int index) {
        return environmentTypes.get(index);
    }

    public EnvironmentType remove(int index) {
        return environmentTypes.remove(index);
    }

    public List<EnvironmentType> getEnvironmentTypes() {
        return environmentTypes;
    }

    @Override
    public EnvironmentTypeBundle clone() {
        return new EnvironmentTypeBundle(this);
    }
}
