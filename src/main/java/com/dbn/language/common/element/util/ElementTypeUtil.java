package com.dbn.language.common.element.util;

import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.impl.NamedElementType;
import com.dbn.language.common.element.path.ParserNode;

public class ElementTypeUtil {
    public static ElementType getEnclosingElementType(ParserNode pathNode, ElementTypeAttribute elementTypeAttribute) {
        ParserNode parentNode = (ParserNode) pathNode.parent;
        while (parentNode != null) {
            ElementType elementType = parentNode.element;
            if (elementType.is(elementTypeAttribute)) return elementType;
            parentNode = (ParserNode) parentNode.parent;
        }
        return null;
    }

    public static NamedElementType getEnclosingNamedElementType(ParserNode pathNode) {
        ParserNode parentNode = (ParserNode) pathNode.parent;
        while (parentNode != null) {
            ElementType elementType = parentNode.element;
            if (elementType instanceof NamedElementType) return (NamedElementType) elementType;
            parentNode = (ParserNode) parentNode.parent;
        }
        return null;
    }
    
}
