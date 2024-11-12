package com.dbn.language.common;

import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.path.LanguageNodeBase;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

@Getter
public class ChameleonTokenType extends SimpleTokenType<ChameleonTokenType> implements ElementType {

    private final DBLanguageDialect injectedLanguage;

    public ChameleonTokenType(@Nullable DBLanguageDialect hostLanguage, DBLanguageDialect injectedLanguage) {
        super(injectedLanguage.getID() + " block", hostLanguage);
        this.injectedLanguage = injectedLanguage;
    }

    @NotNull
    @Override
    public DBLanguage getLanguage() {
        return getLanguageDialect().getBaseLanguage();
    }

    @Override
    public DBLanguageDialect getLanguageDialect() {
        return (DBLanguageDialect) super.getLanguage();
    }

    @NotNull
    @Override
    public String getName() {
        return getId();
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public boolean is(ElementTypeAttribute attribute) {
        return false;
    }

    @Override
    public boolean set(ElementTypeAttribute status, boolean value) {
        throw new AbstractMethodError("Operation not allowed");
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public boolean isVirtualObject() {
        return false;
    }

    @Override
    public PsiElement createPsiElement(ASTNode astNode) {
        return new ASTWrapperPsiElement(astNode);
    }

    @Override
    public boolean isWrappingBegin(LeafElementType elementType) {
        return false;
    }

    @Override
    public boolean isWrappingEnd(LeafElementType elementType) {
        return false;
    }

    @Override
    public boolean isWrappingBegin(TokenType tokenType) {
        return false;
    }

    @Override
    public boolean isWrappingEnd(TokenType tokenType) {
        return false;
    }

    @Override
    public TokenType getTokenType() {
        return null;
    }

    @Override
    public int getIndexInParent(LanguageNodeBase node) {
        return 0;
    }
}
