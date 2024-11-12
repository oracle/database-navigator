package com.dbn.language.common.element.impl;

import com.dbn.common.util.Commons;
import com.dbn.common.util.Strings;
import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.cache.OneOfElementTypeLookupCache;
import com.dbn.language.common.element.parser.BranchCheck;
import com.dbn.language.common.element.parser.impl.OneOfElementTypeParser;
import com.dbn.language.common.element.util.ElementTypeDefinitionException;
import com.dbn.language.common.psi.SequencePsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Unsafe.cast;

public final class OneOfElementType extends ElementTypeBase {
    public ElementTypeRef[] children;
    private boolean sortable;
    private boolean sorted;
    private boolean basic;

    public OneOfElementType(ElementTypeBundle bundle, ElementTypeBase parent, String id, Element def) throws ElementTypeDefinitionException {
        super(bundle, parent, id, def);
    }

    @Override
    protected void loadDefinition(Element def) throws ElementTypeDefinitionException {
        super.loadDefinition(def);
        String tokenIds = stringAttribute(def, "tokens");
        if (Strings.isNotEmptyOrSpaces(tokenIds)) {
            basic = true;
            String id = getId();

            String[] tokens = tokenIds.split(",");
            this.children = new ElementTypeRef[tokens.length];

            ElementTypeRef previous = null;
            for (int i=0; i<tokens.length; i++) {
                String tokenTypeId = tokens[i].trim();

                TokenElementType tokenElementType = new TokenElementType(bundle, this, tokenTypeId, id);
                children[i] = new ElementTypeRef(previous, this, tokenElementType, false, 0, null);
                previous = this.children[i];
            }
            sortable = false;
        } else {
            List<Element> children = def.getChildren();
            this.children = new ElementTypeRef[children.size()];

            ElementTypeRef previous = null;
            for (int i=0; i<children.size(); i++) {
                Element child = children.get(i);
                String type = child.getName();
                ElementTypeBase elementType = bundle.resolveElementDefinition(child, type, this);
                double version = Double.parseDouble(Commons.nvl(stringAttribute(child, "version"), "0"));
                Set<BranchCheck> branchChecks = parseBranchChecks(stringAttribute(child, "branch-check"));

                this.children[i] = new ElementTypeRef(previous, this, elementType, false, version, branchChecks);
                previous = this.children[i];

            }
            sortable = getBooleanAttribute(def, "sortable");
        }

        if (children == null || children.length == 0) {
            // TODO assert at least 2 children
            throw new ElementTypeDefinitionException("[" + getLanguageDialect().getID() + "] Invalid one-of definition (id=" + getId() + "). Element should contain at least 2 elements.");
        }
    }

    @Override
    protected OneOfElementTypeLookupCache createLookupCache() {
        return new OneOfElementTypeLookupCache(this);
    }

    @NotNull
    @Override
    protected OneOfElementTypeParser createParser() {
        return new OneOfElementTypeParser(this);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @NotNull
    @Override
    public String getName() {
        return "one-of (" + getId() + ")";
    }

    @Override
    public PsiElement createPsiElement(ASTNode astNode) {
        return new SequencePsiElement<>(astNode, this);
    }

    public void sort() {
        if (sortable && !sorted) {
            sorted = true;
            Arrays.sort(children, ONE_OF_COMPARATOR);
            relink();
        }
    }

    private void relink() {
        ElementTypeRef previous = null;
        for (ElementTypeRef child : children) {
            child.setPrevious(previous);
            child.setNext(null);
            if (previous != null) {
                previous.setNext(child);
            }
            previous = child;
        }
    }

    private static final Comparator<ElementTypeRef> ONE_OF_COMPARATOR = (o1, o2) -> {
        int i1 = o1.elementType.cache.startsWithIdentifier() ? 1 : 2;
        int i2 = o2.elementType.cache.startsWithIdentifier() ? 1 : 2;
        return i2-i1;
    };

    public ElementTypeRef getFirstChild() {
        return children[0];
    }

    @Override
    public void collectLeafElements(Set<LeafElementType> bucket) {
        super.collectLeafElements(bucket);
        if (basic) {
            for (ElementTypeRef child : children) {
                bucket.add(cast(child.elementType));
            }
        }
    }
}
