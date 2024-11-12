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
