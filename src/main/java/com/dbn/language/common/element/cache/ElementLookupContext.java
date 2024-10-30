package com.dbn.language.common.element.cache;

import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.impl.ElementTypeRef;
import com.dbn.language.common.element.impl.NamedElementType;
import com.dbn.language.common.element.parser.Branch;
import com.dbn.language.common.element.path.ParserNode;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ElementLookupContext {
    public static double MAX_DB_VERSION = 9999;
    private final Set<NamedElementType> scannedElements = new HashSet<>();
    private final Set<ElementTypeAttribute> breakOnAttributes = new HashSet<>();
    private final Map<Branch, NamedElementType> branchMarkers = new HashMap<>();
    protected Set<Branch> branches;

    private double databaseVersion = MAX_DB_VERSION;

    @Deprecated
    public ElementLookupContext() {}

    public ElementLookupContext(double version) {
        this.databaseVersion = version;
    }

    public ElementLookupContext(Set<Branch> branches, double version) {
        this.branches = branches;
        this.databaseVersion = version;
    }

    public void addBreakOnAttribute(ElementTypeAttribute attribute) {
        breakOnAttributes.add(attribute);
    }

    public boolean check(ElementTypeRef elementTypeRef) {
        return elementTypeRef.check(branches, databaseVersion);
    }

    public void addBranchMarker(ASTNode astNode, Branch branch) {
        NamedElementType namedElementType = getNamedElement(astNode);
        if (namedElementType != null) {
            branchMarkers.put(branch, namedElementType);
            this.branches = branchMarkers.keySet();
        }
    }

    public void addBranchMarker(ParserNode pathNode, Branch branch) {
        NamedElementType namedElementType = getNamedElement(pathNode);
        if (namedElementType != null) {
            branchMarkers.put(branch, namedElementType);
            this.branches = branchMarkers.keySet();
        }
    }

    @Nullable
    private static NamedElementType getNamedElement(ASTNode astNode) {
        astNode = astNode.getTreeParent();
        while (astNode != null) {
            IElementType elementType = astNode.getElementType();
            if (elementType instanceof NamedElementType) {
                return (NamedElementType) elementType;
            }
            astNode = astNode.getTreeParent();
        }
        return null;
    }

    @Nullable
    private static NamedElementType getNamedElement(ParserNode pathNode) {
        pathNode = (ParserNode) pathNode.parent;
        while (pathNode != null) {
            ElementType elementType = pathNode.element;
            if (elementType instanceof NamedElementType) {
                return (NamedElementType) elementType;
            }
            pathNode = (ParserNode) pathNode.parent;
        }
        return null;
    }

    public void removeBranchMarkers(ParserNode pathNode) {
        ElementType elementType = pathNode.element;
        if (elementType instanceof NamedElementType) {
            removeBranchMarkers((NamedElementType) elementType);
        }
    }

    public void removeBranchMarkers(NamedElementType elementType) {
        if (branchMarkers.size() > 0 && branchMarkers.containsValue(elementType)) {
            branchMarkers.keySet().removeIf(key -> branchMarkers.get(key) == elementType);
        }
        branches = branchMarkers.size() == 0 ? null : branchMarkers.keySet();
    }


    public ElementLookupContext reset() {
        scannedElements.clear();
        return this;
    }

    boolean isScanned(NamedElementType elementType) {
        return scannedElements.contains(elementType);
    }

    void markScanned(NamedElementType elementType) {
        scannedElements.add(elementType);
    }

    public boolean isBreakOnAttribute(ElementTypeAttribute attribute) {
        return breakOnAttributes.contains(attribute);
    }
}
