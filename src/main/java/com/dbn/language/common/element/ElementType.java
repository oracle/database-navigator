package com.dbn.language.common.element;

import com.dbn.code.common.style.formatting.FormattingDefinition;
import com.dbn.common.property.PropertyHolder;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.path.LanguageNodeBase;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;

import javax.swing.Icon;
import java.util.Set;

public interface ElementType extends PropertyHolder<ElementTypeAttribute>{

    String getId();

    String getName();

    default String getDebugName() {
        return getName();
    }

    String getDescription();

    Icon getIcon();

    DBLanguage getLanguage();

    DBLanguageDialect getLanguageDialect();

    boolean isLeaf();

    boolean isVirtualObject();

    PsiElement createPsiElement(ASTNode astNode);

    FormattingDefinition getFormatting();

    void setDefaultFormatting(FormattingDefinition defaults);

    boolean isWrappingBegin(LeafElementType elementType);

    boolean isWrappingBegin(TokenType tokenType);

    boolean isWrappingEnd(LeafElementType elementType);

    boolean isWrappingEnd(TokenType tokenType);

    int getIndexInParent(LanguageNodeBase node);

    TokenType getTokenType();

    default void collectLeafElements(Set<LeafElementType> leafElementTypes) {};
}
