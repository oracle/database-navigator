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

package com.dbn.common.path;

import lombok.Getter;

@Getter
public class NodeBase<T> implements Node<T> {
    public NodeBase<T> parent;
    public final T element;

    public NodeBase(T element, NodeBase<T> parent) {
        this.element = element;
        this.parent = parent;
    }

    @Override
    public boolean isRecursive() {
        Node<T> node = this.parent;
        while (node != null) {
            if (node.getElement() == this.element) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    @Override
    public boolean isAncestor(T element) {
        if (this.element == element) {
            return true;
        }
        Node<T> node = this.parent;
        while (node != null) {
            if (node.getElement() == element) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        Node<T> parent = this;
        while (parent != null) {
            buffer.insert(0, '/');
            buffer.insert(0, parent.getElement());
            parent = parent.getParent();
        }
        return buffer.toString();
    }

    @Override
    public void detach() {
        parent = null;
    }
}
