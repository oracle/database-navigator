package com.dbn.language.common.element.path;

import com.dbn.common.path.Node;
import com.dbn.language.common.element.impl.ElementTypeBase;

public interface LanguageNode extends Node<ElementTypeBase> {

    @Override
    LanguageNode getParent();

    int getIndexInParent();
}
