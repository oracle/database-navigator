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

package com.dbn.assistant.credential.local;

import com.dbn.common.util.Cloneable;
import com.dbn.common.util.CollectionUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class LocalCredentialBundle implements Iterable<LocalCredential>, Cloneable {
  private final List<LocalCredential> elements = new ArrayList<>();

  public LocalCredentialBundle(LocalCredentialBundle source) {
    this(source.getElements());
  }

  public LocalCredentialBundle(List<LocalCredential> elements) {
    setElements(elements);
  }

  public void setElements(List<LocalCredential> credentials) {
    this.elements.clear();
    CollectionUtil.cloneElements(credentials, this.elements);
  }

  @Override
  public Iterator<LocalCredential> iterator() {
    return elements.iterator();
  }

  public void clear() {
    elements.clear();
  }

  public void add(LocalCredential credential) {
    elements.add(credential);
  }

  public void add(int index, LocalCredential credential) {
    elements.add(index, credential);
  }


  public int size() {
    return elements.size();
  }

  public LocalCredential get(int index) {
    return elements.get(index);
  }

  public LocalCredential remove(int index) {
    return elements.remove(index);
  }

  @Override
  public LocalCredentialBundle clone() {
    return new LocalCredentialBundle(this);
  }
}
