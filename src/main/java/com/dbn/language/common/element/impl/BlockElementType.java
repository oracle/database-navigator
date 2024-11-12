package com.dbn.language.common.element.impl;

import com.dbn.common.options.setting.Settings;
import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.cache.BlockElementTypeLookupCache;
import com.dbn.language.common.element.parser.impl.BlockElementTypeParser;
import com.dbn.language.common.element.util.ElementTypeDefinitionException;
import com.dbn.language.common.psi.BlockPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class BlockElementType extends SequenceElementType {
    public static final int INDENT_NONE = 0;
    public static final int INDENT_NORMAL = 1;

    public int indent;

    public BlockElementType(ElementTypeBundle bundle, ElementTypeBase parent, String id, Element def) throws ElementTypeDefinitionException {
        super(bundle, parent, id, def);
    }

    @Override
    public BlockElementTypeLookupCache createLookupCache() {
        return new BlockElementTypeLookupCache(this);
    }

    @NotNull
    @Override
    public BlockElementTypeParser createParser() {
        return new BlockElementTypeParser(this);
    }

    @Override
    protected void loadDefinition(Element def) throws ElementTypeDefinitionException {
        super.loadDefinition(def);
        String indentString = Settings.stringAttribute(def, "indent");
        if (indentString != null) {
            indent = Objects.equals(indentString, "NORMAL") ? INDENT_NORMAL : INDENT_NONE;
        }
    }

    @Override
    public PsiElement createPsiElement(ASTNode astNode) {
        return new BlockPsiElement(astNode, this);
    }

    @NotNull
    @Override
    public String getName() {
        return "block (" + getId() + ")";
    }
}
