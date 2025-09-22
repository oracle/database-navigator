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

package com.dbn.assistant.credential;

import com.dbn.common.util.CollectionUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.util.Lists.first;

@Getter
@Setter
public class AssistantCredentialBundle {
    private final List<AssistantCredential> elements = new ArrayList<>();

    public AssistantCredentialBundle() {}

    public AssistantCredentialBundle(AssistantCredentialBundle source) {
        this(source.getElements());
    }

    public AssistantCredentialBundle(List<AssistantCredential> elements) {
        setCredentials(elements);
    }

    public void setCredentials(List<AssistantCredential> credentials) {
        this.elements.clear();
        CollectionUtil.cloneElements(credentials, this.elements);
    }

    public void addCredential(AssistantCredential credential) {
        this.elements.add(credential);
    }

    public int size() {
        return elements.size();
    }

    public AssistantCredential getCredential(String id) {
        return first(elements, c -> c.getId().equals(id));
    }

    public AssistantCredential getCredential(int index) {
        return elements.get(index);
    }

    public void initSecrets() {
        for (AssistantCredential element : elements) {
            element.initSecrets();
        }
    }
}
