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
import com.dbn.language.common.element.path.ParserNode;
import com.dbn.language.common.element.util.ElementTypeDefinitionException;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.dbn.language.common.element.util.ElementTypeAttribute.STATEMENT;

@Getter
@Setter
public abstract class LeafElementType extends ElementTypeBase implements Indexable {
    private TokenType tokenType;
    private boolean optional;
    private int idx;

    LeafElementType(ElementTypeBundle bundle, ElementType parent, String id, Element def) throws ElementTypeDefinitionException {
        super(bundle, parent, id, def);
        idx = bundle.nextIndex();
        bundle.registerElement(this);
    }

    LeafElementType(ElementTypeBundle bundle, ElementType parent, String id, String description) {
        super(bundle, parent, id, description);
        idx = bundle.nextIndex();
        bundle.registerElement(this);
    }

    @Override
    public int index() {
        return idx;
    }

    public void registerLeaf() {
        getParent().getLookupCache().registerLeaf(this, this);
    }

    public abstract boolean isSameAs(LeafElementType elementType);

    public abstract boolean isIdentifier();

    @Override
    public boolean isLeaf() {
        return true;
    }

    public static ElementType getPreviousElement(LanguageNode pathNode) {
        int position = 0;
        while (pathNode != null) {
            ElementType elementType = pathNode.getElement();
            if (elementType instanceof SequenceElementType) {
                SequenceElementType sequenceElementType = (SequenceElementType) elementType;
                if (position > 0 ) {
                    return sequenceElementType.getChild(position-1).getElementType();
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

            if (elementType instanceof SequenceElementType) {
                SequenceElementType sequenceElementType = (SequenceElementType) elementType;

                int elementsCount = sequenceElementType.getChildCount();

                if (position < elementsCount) {
                    ElementTypeRef element = sequenceElementType.getChild(position);
                    while (element != null) {
                        if (context.check(element)) {
                            element.getLookupCache().captureFirstPossibleLeafs(context.reset(), possibleLeafs);
                            if (!element.isOptional()) {
                                pathNode = null;
                                break;
                            }
                        }
                        element = element.getNext();
                    }
                } else if (elementType instanceof NamedElementType){
                    context.removeBranchMarkers((NamedElementType) elementType);
                }
            } else if (elementType instanceof IterationElementType) {
                IterationElementType iterationElementType = (IterationElementType) elementType;

                TokenElementType[] separatorTokens = iterationElementType.getSeparatorTokens();
                if (separatorTokens != null) possibleLeafs.addAll(Arrays.asList(separatorTokens));

                ElementTypeLookupCache<?> lookupCache = iterationElementType.getIteratedElementType().getLookupCache();
                lookupCache.captureFirstPossibleLeafs(context.reset(), possibleLeafs);

            } else if (elementType instanceof QualifiedIdentifierElementType) {
                QualifiedIdentifierElementType qualifiedIdentifierElementType = (QualifiedIdentifierElementType) elementType;
                if (this == qualifiedIdentifierElementType.getSeparatorToken()) break;

            } else if (elementType instanceof ChameleonElementType) {
                ChameleonElementType chameleonElementType = (ChameleonElementType) elementType;
                ElementTypeBundle elementTypeBundle = chameleonElementType.getParentLanguage().getParserDefinition().getParser().getElementTypes();
                ElementTypeLookupCache<?> lookupCache = elementTypeBundle.getRootElementType().getLookupCache();
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

    public boolean isNextPossibleToken(TokenType tokenType, ParserNode pathNode, ParserContext context) {
        return isNextToken(tokenType, pathNode, context, false);
    }

    public boolean isNextRequiredToken(TokenType tokenType, ParserNode pathNode, ParserContext context) {
        return isNextToken(tokenType, pathNode, context, true);
    }

    private boolean isNextToken(TokenType tokenType, ParserNode pathNode, ParserContext context, boolean required) {
        int position = -1;
        while (pathNode != null) {
            ElementType elementType = pathNode.getElement();

            if (elementType instanceof SequenceElementType) {
                SequenceElementType sequenceElementType = (SequenceElementType) elementType;

                int elementsCount = sequenceElementType.getChildCount();
                if (position == -1) {
                    position = pathNode.getCursorPosition() + 1;
                }

                //int position = sequenceElementType.indexOf(this) + 1;
/*
                int position = pathNode.getCursorPosition();
                if (pathNode.getCurrentOffset() < context.getBuilder().getCurrentOffset()) {
                    position++;
                }
*/
                if (position < elementsCount) {
                    ElementTypeRef element = sequenceElementType.getChild(position);
                    while (element != null) {
                        ElementTypeLookupCache lookupCache = element.getLookupCache();
                        if (required) {
                            if (lookupCache.isFirstRequiredToken(tokenType) && !element.isOptional()) {
                                return true;
                            }
                        } else {
                            if (lookupCache.isFirstPossibleToken(tokenType)) {
                                return true;
                            }
                        }

                        if (!element.isOptional()/* && !child.isOptionalFromHere()*/) {
                            return false;
                        }
                        element = element.getNext();
                    }
                }
            } else if (elementType instanceof IterationElementType) {
                IterationElementType iterationElementType = (IterationElementType) elementType;
                TokenElementType[] separatorTokens = iterationElementType.getSeparatorTokens();
                if (separatorTokens == null) {
                    ElementTypeLookupCache<?> lookupCache = iterationElementType.getIteratedElementType().getLookupCache();
                    if (required ?
                            lookupCache.isFirstRequiredToken(tokenType) :
                            lookupCache.isFirstPossibleToken(tokenType)) {
                        return true;
                    }
                }
            } else if (elementType instanceof QualifiedIdentifierElementType) {
                QualifiedIdentifierElementType qualifiedIdentifierElementType = (QualifiedIdentifierElementType) elementType;
                if (this == qualifiedIdentifierElementType.getSeparatorToken()) {
                    break;
                }
            } else if (elementType instanceof WrapperElementType) {
                WrapperElementType wrapperElementType = (WrapperElementType) elementType;
                return wrapperElementType.getEndTokenElement().getTokenType() == tokenType;
            }

            position = pathNode.getIndexInParent() + 1;
            pathNode = pathNode.getParent();
        }
        return false;
    }

    public Set<LeafElementType> getNextRequiredLeafs(LanguageNode pathNode, ParserContext context) {
        Set<LeafElementType> requiredLeafs = new HashSet<>();
        int position = 0;
        while (pathNode != null) {
            ElementType elementType = pathNode.getElement();

            if (elementType instanceof SequenceElementType) {
                SequenceElementType sequenceElementType = (SequenceElementType) elementType;

                ElementTypeRef element = sequenceElementType.getChild(position + 1);
                while (element != null) {
                    if (!element.isOptional()) {
                        ElementTypeLookupCache<?> lookupCache = element.getLookupCache();
                        requiredLeafs.addAll(lookupCache.getFirstRequiredLeafs());
                        pathNode = null;
                        break;
                    }
                    element = element.getNext();
                }
            } else if (elementType instanceof IterationElementType) {
                IterationElementType iteration = (IterationElementType) elementType;
                TokenElementType[] separatorTokens = iteration.getSeparatorTokens();
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
    public void collectLeafElements(Set<LeafElementType> bucket) {
        super.collectLeafElements(bucket);
        bucket.add(this);
    }
}
