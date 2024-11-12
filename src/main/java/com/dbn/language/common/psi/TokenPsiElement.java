package com.dbn.language.common.psi;

import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.impl.TokenElementType;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.lookup.PsiLookupAdapter;
import com.dbn.object.type.DBObjectType;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Objects;
import java.util.function.Consumer;

public class TokenPsiElement extends LeafPsiElement<TokenElementType> {
    public TokenPsiElement(ASTNode astNode, TokenElementType elementType) {
        super(astNode, elementType);
    }

    @Override
    @Nullable
    public BasePsiElement findPsiElement(PsiLookupAdapter lookupAdapter, int scopeCrossCount) {
        return lookupAdapter.matches(this) ? this : null;
    }
    @Override
    public void collectPsiElements(PsiLookupAdapter lookupAdapter, int scopeCrossCount, @NotNull Consumer<BasePsiElement> consumer) {}

    @Override
    public void collectExecVariablePsiElements(@NotNull Consumer<ExecVariablePsiElement> consumer) {}

    @Override
    public void collectSubjectPsiElements(@NotNull Consumer<IdentifierPsiElement> consumer) {}

    @Override
    public NamedPsiElement findNamedPsiElement(String id) {return null;}
    @Override
    public BasePsiElement findPsiElementBySubject(ElementTypeAttribute attribute, CharSequence subjectName, DBObjectType subjectType) {return null;}


    /*********************************************************
     *                       PsiReference                    *
     *********************************************************/
    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        return true;
    }

    @Override
    public boolean isSoft() {
        return false;
    }

    /*********************************************************
     *                       ItemPresentation                *
     *********************************************************/
    @Override
    public String getPresentableText() {
        return getTokenType().getValue();
    }

    @Override
    @Nullable
    public Icon getIcon(boolean open) {
        return null;
    }

    @Override
    @Nullable
    public TextAttributesKey getTextAttributesKey() {
        return null;
    }

    @Override
    public boolean hasErrors() {
        return false;
    }

    @Override
    public boolean matches(BasePsiElement basePsiElement, MatchType matchType) {
        if (basePsiElement instanceof TokenPsiElement) {
            TokenPsiElement remote = (TokenPsiElement) basePsiElement;
            TokenType localTokenType = getTokenType();
            TokenType remoteTokenType = remote.getTokenType();
            if (localTokenType == remoteTokenType) {
                if (matchType == MatchType.SOFT) {
                    return true;
                } else {
                    if (localTokenType.isNumeric() || localTokenType.isLiteral()) {
                        return Objects.equals(getChars(), remote.getChars());
                    } else {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public TokenType getTokenType() {
        return elementType.tokenType;
    }

    @Override
    public boolean isCharacterToken() {
        return getTokenType().isCharacter();
    }

    @Override
    public boolean isToken(TokenType tokenType) {
        return getTokenType() == tokenType;
    }
}
