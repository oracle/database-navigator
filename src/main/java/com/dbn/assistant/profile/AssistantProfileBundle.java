/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.assistant.profile;

import com.dbn.common.util.Cloneable;
import com.dbn.common.util.CollectionUtil;
import com.dbn.common.util.Lists;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AssistantProfileBundle implements Iterable<AssistantProfile>, Cloneable {
    private final List<AssistantProfile> elements = new ArrayList<>();

    public AssistantProfileBundle(AssistantProfileBundle source) {
        this(source.getElements());
    }

    public AssistantProfileBundle(List<AssistantProfile> elements) {
        setElements(elements);
    }

    public void setElements(List<AssistantProfile> profiles) {
        this.elements.clear();
        CollectionUtil.cloneElements(profiles, this.elements);
    }

    @Override
    public Iterator<AssistantProfile> iterator() {
        return elements.iterator();
    }

    public void clear() {
        elements.clear();
    }

    public void add(AssistantProfile profile) {
        elements.add(profile);
    }

    public void add(int index, AssistantProfile profile) {
        elements.add(index, profile);
    }


    public int size() {
        return elements.size();
    }

    public AssistantProfile get(String profileName) {
        return Lists.first(elements, p -> p.getName().equals(profileName));
    }

    public AssistantProfile get(int index) {
        return elements.get(index);
    }

    public AssistantProfile remove(int index) {
        return elements.remove(index);
    }

    @Override
    public AssistantProfileBundle clone() {
        return new AssistantProfileBundle(this);
    }
}
