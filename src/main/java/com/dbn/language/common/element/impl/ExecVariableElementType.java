package com.dbn.language.common.element.impl;

import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.cache.ExecVariableElementTypeLookupCache;
import com.dbn.language.common.element.parser.impl.ExecVariableElementTypeParser;
import com.dbn.language.common.element.util.ElementTypeDefinitionException;
import com.dbn.language.common.psi.ExecVariablePsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;


public class ExecVariableElementType extends LeafElementType {

    public ExecVariableElementType(ElementTypeBundle bundle, ElementTypeBase parent, String id, Element def) throws ElementTypeDefinitionException {
        super(bundle, parent, id, def);
        tokenType = bundle.getTokenTypeBundle().getVariable();
    }

    @Override
    public ExecVariableElementTypeLookupCache createLookupCache() {
        return new ExecVariableElementTypeLookupCache(this);
    }

    @NotNull
    @Override
    public ExecVariableElementTypeParser createParser() {
        return new ExecVariableElementTypeParser(this);
    }

    @Override
    protected void loadDefinition(Element def) throws ElementTypeDefinitionException {
        super.loadDefinition(def);
    }

    @Override
    public PsiElement createPsiElement(ASTNode astNode) {
        return new ExecVariablePsiElement(astNode, this);
    }

    @NotNull
    @Override
    public String getName() {
        return "variable (" + getId() + ")";
    }

    public String toString() {
        return "variable (" + getId() + ")";
    }

    @Override
    public boolean isSameAs(LeafElementType elementType) {
        return elementType instanceof ExecVariableElementType;
    }

    @Override
    public boolean isIdentifier() {
        return true;
    }
}
