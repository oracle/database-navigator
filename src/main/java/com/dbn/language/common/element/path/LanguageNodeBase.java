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

package com.dbn.language.common.element.path;

import com.dbn.common.path.NodeBase;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import lombok.Getter;

@Getter
public class LanguageNodeBase extends NodeBase<ElementTypeBase> implements LanguageNode {
    public LanguageNodeBase(ElementTypeBase elementType, LanguageNodeBase parent) {
        super(elementType, parent);
    }

    @Override
    public LanguageNodeBase getParent() {
        return (LanguageNodeBase) super.parent;
    }

    public LanguageNode getParent(ElementTypeAttribute attribute) {
        LanguageNodeBase pathNode = this;
        while (pathNode != null) {
            if (pathNode.element.is(attribute)) {
                return pathNode;
            }
            pathNode = pathNode.getParent();
        }
        return null;

    }

    public int getIndexInParent() {
        return this.element.getIndexInParent(this);
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        LanguageNodeBase parent = this;
        while (parent != null) {
            buffer.insert(0, '/');
            buffer.insert(0, parent.element.getId());
            parent = parent.getParent();
        }
        return buffer.toString();
    }
}
