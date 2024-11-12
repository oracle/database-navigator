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
