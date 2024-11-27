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
