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

package com.dbn.language.common.element.impl;

import com.dbn.common.index.Indexable;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.ChameleonElementType;
import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.cache.ElementLookupContext;
import com.dbn.language.common.element.cache.ElementTypeLookupCache;
import com.dbn.language.common.element.parser.ParserContext;
import com.dbn.language.common.element.path.LanguageNode;
import com.dbn.language.common.element.path.LanguageNodeBase;
import com.dbn.language.common.element.path.ParserNode;
import com.dbn.language.common.element.util.ElementTypeDefinitionException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.dbn.language.common.element.impl.ElementTypeCache.getUnwrappedFirstPossibleLeafs;
import static com.dbn.language.common.element.util.ElementTypeAttribute.STATEMENT;
import static com.dbn.language.common.element.util.ElementTypeAttribute.SURROGATE_LEAD;
import static java.util.Collections.disjoint;

public abstract class LeafElementType extends ElementTypeBase implements Indexable {
    public TokenType tokenType;

    public boolean optional;
    private final int idx;

    public Set<LeafElementType> surrogateFor;
    public Set<LeafElementType> surrogatedBy;

    LeafElementType(ElementTypeBundle bundle, ElementTypeBase parent, String id, Element def) throws ElementTypeDefinitionException {
        super(bundle, parent, id, def);
        idx = bundle.nextIndex();
        bundle.registerElement(this);
    }

    LeafElementType(ElementTypeBundle bundle, ElementTypeBase parent, String id) {
        super(bundle, parent, id);
        idx = bundle.nextIndex();
        bundle.registerElement(this);
    }

    public boolean isSurrogateFor(ElementTypeBase elementType) {
        if (surrogateFor == null) return false;

        if (elementType instanceof LeafElementType leafElementType && !leafElementType.is(SURROGATE_LEAD)) {
            return surrogateFor.contains(leafElementType);
        }

        Set<LeafElementType> firstPossibleLeafs = getUnwrappedFirstPossibleLeafs(elementType);
        return !disjoint(surrogateFor, firstPossibleLeafs);
    }

    @Override
    public int index() {
        return idx;
    }

    public void registerLeaf() {
        parent.cache.registerLeaf(this, this);
    }

    @Override
    public TokenType getTokenType() {
        return tokenType;
    }


    public abstract boolean isSameAs(LeafElementType elementType);

    public abstract boolean isIdentifier();

    @Override
    public boolean isLeaf() {
        return true;
    }

    public static ElementType getPreviousElement(LanguageNodeBase pathNode) {
        int position = 0;
        while (pathNode != null) {
            ElementType elementType = pathNode.element;
            if (elementType instanceof SequenceElementType sequenceElementType) {
                if (position > 0 ) {
                    return sequenceElementType.children[position-1].elementType;
                }
            }
            position = pathNode.getIndexInParent();
            pathNode = pathNode.getParent();
        }
        return null;
    }

    public Set<LeafElementType> getNextPossibleLeafs(LanguageNode pathNode, @NotNull ElementLookupContext context) {
        Set<LeafElementType> possibleLeafs = new HashSet<>();
        int position = 1;
        while (pathNode != null) {
            ElementType elementType = pathNode.getElement();

            if (elementType instanceof SequenceElementType sequenceElementType) {
                int elementsCount = sequenceElementType.children.length;

                if (position < elementsCount) {
                    ElementTypeRef element = sequenceElementType.children[position];
                    while (element != null) {
                        if (context.check(element)) {
                            element.elementType.cache.captureFirstPossibleLeafs(context.reset(), possibleLeafs);
                            if (!element.optional) {
                                pathNode = null;
                                break;
                            }
                        }
                        element = element.next;
                    }
                } else if (elementType instanceof NamedElementType){
                    context.removeBranchMarkers((NamedElementType) elementType);
                }
            } else if (elementType instanceof IterationElementType iterationElementType) {
                TokenElementType[] separatorTokens = iterationElementType.separatorTokens;
                if (separatorTokens != null) possibleLeafs.addAll(Arrays.asList(separatorTokens));

                ElementTypeLookupCache<?> lookupCache = iterationElementType.iteratedElement.cache;
                lookupCache.captureFirstPossibleLeafs(context.reset(), possibleLeafs);

            } else if (elementType instanceof QualifiedIdentifierElementType qualifiedIdentifierElementType) {
                if (this == qualifiedIdentifierElementType.separatorToken) break;

            } else if (elementType instanceof ChameleonElementType chameleonElementType) {
                ElementTypeBundle elementTypeBundle = chameleonElementType.getParentLanguage().getParserDefinition().getParser().getElementTypes();
                ElementTypeLookupCache<?> lookupCache = elementTypeBundle.getRootElementType().cache;
                possibleLeafs.addAll(lookupCache.getFirstPossibleLeafs());
            }
            if (pathNode != null) {
                ElementType pathElementType = pathNode.getElement();
                if (pathElementType != null && pathElementType.is(STATEMENT) && context.isBreakOnAttribute(STATEMENT)) break;

                position = pathNode.getIndexInParent() + 1;
                pathNode = pathNode.getParent();
            }
        }
        return possibleLeafs;
    }

    public boolean isNextPossibleToken(TokenType tokenType, ParserNode pathNode) {
        return isNextToken(tokenType, pathNode, false);
    }

    public boolean isNextRequiredToken(TokenType tokenType, ParserNode pathNode) {
        return isNextToken(tokenType, pathNode, true);
    }

    private boolean isNextToken(TokenType tokenType, ParserNode pathNode, boolean required) {
        int index = -1;
        while (pathNode != null) {
            ElementType elementType = pathNode.element;

            if (elementType instanceof SequenceElementType sequenceElementType) {

                int elementsCount = sequenceElementType.children.length;
                if (index == -1) {
                    index = pathNode.elementIndex + 1;
                }

                //int position = sequenceElementType.indexOf(this) + 1;
/*
                int position = pathNode.getCursorPosition();
                if (pathNode.getCurrentOffset() < context.getBuilder().getCurrentOffset()) {
                    position++;
                }
*/
                if (index < elementsCount) {
                    ElementTypeRef element = sequenceElementType.children[index];
                    while (element != null) {
                        ElementTypeLookupCache lookupCache = element.elementType.cache;
                        if (required) {
                            if (lookupCache.isFirstRequiredToken(tokenType) && !element.optional) {
                                return true;
                            }
                        } else {
                            if (lookupCache.isFirstPossibleToken(tokenType)) {
                                return true;
                            }
                        }

                        if (!element.optional/* && !child.isOptionalFromHere()*/) {
                            return false;
                        }
                        element = element.next;
                    }
                }
            } else if (elementType instanceof IterationElementType iterationElementType) {
                TokenElementType[] separatorTokens = iterationElementType.separatorTokens;
                if (separatorTokens == null) {
                    ElementTypeLookupCache<?> lookupCache = iterationElementType.iteratedElement.cache;
                    if (required ?
                            lookupCache.isFirstRequiredToken(tokenType) :
                            lookupCache.isFirstPossibleToken(tokenType)) {
                        return true;
                    }
                }
            } else if (elementType instanceof QualifiedIdentifierElementType qualifiedIdentifierElementType) {
                if (this == qualifiedIdentifierElementType.separatorToken) {
                    break;
                }
            }

            if (elementType instanceof WrapperElementType wrapperElementType) {
                return wrapperElementType.getEndTokenElement().tokenType == tokenType;
            }

            if (elementType instanceof OneOfElementType oneOfElementType && !required) {
                ElementTypeLookupCache<?> lookupCache = oneOfElementType.cache;
                if (lookupCache.isFirstPossibleToken(tokenType)) {
                    return true;
                }
            }

            index = pathNode.getIndexInParent() + 1;
            pathNode = (ParserNode) pathNode.parent;
        }
        return false;
    }

    public Set<LeafElementType> getNextRequiredLeafs(LanguageNode pathNode, ParserContext context) {
        Set<LeafElementType> requiredLeafs = new HashSet<>();
        int position = 0;
        while (pathNode != null) {
            ElementType elementType = pathNode.getElement();

            if (elementType instanceof SequenceElementType sequenceElementType) {

                ElementTypeRef element = sequenceElementType.children[position + 1];
                while (element != null) {
                    if (!element.optional) {
                        ElementTypeLookupCache<?> lookupCache = element.elementType.cache;
                        requiredLeafs.addAll(lookupCache.getFirstRequiredLeafs());
                        pathNode = null;
                        break;
                    }
                    element = element.next;
                }
            } else if (elementType instanceof IterationElementType iteration) {
                TokenElementType[] separatorTokens = iteration.separatorTokens;
                Collections.addAll(requiredLeafs, separatorTokens);
            }
            if (pathNode != null) {
                position = pathNode.getIndexInParent();
                pathNode = pathNode.getParent();
            }
        }
        return requiredLeafs;
    }

    @Override
    public void collectAnonymousLeafs(Set<LeafElementType> bucket) {
        super.collectAnonymousLeafs(bucket);
        bucket.add(this);
    }
}
