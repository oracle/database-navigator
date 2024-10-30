package com.dbn.language.common.element.impl;

import com.dbn.code.common.lookup.LookupItemBuilderProvider;
import com.dbn.code.common.lookup.TokenLookupItemBuilder;
import com.dbn.common.util.Strings;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.TokenTypeCategory;
import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.cache.ElementLookupContext;
import com.dbn.language.common.element.cache.ElementTypeLookupCache;
import com.dbn.language.common.element.cache.TokenElementTypeLookupCache;
import com.dbn.language.common.element.parser.ParserContext;
import com.dbn.language.common.element.parser.impl.TokenElementTypeParser;
import com.dbn.language.common.element.path.LanguageNode;
import com.dbn.language.common.element.util.ElementTypeDefinitionException;
import com.dbn.language.common.psi.TokenPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

import static com.dbn.common.options.setting.Settings.stringAttribute;

public final class TokenElementType extends LeafElementType implements LookupItemBuilderProvider {
    public static final String SEPARATOR = "SEPARATOR";

    private final TokenLookupItemBuilder lookupItemBuilder = new TokenLookupItemBuilder(this);
    private TokenTypeCategory flavor;
    private String text;

    public TokenElementType(ElementTypeBundle bundle, ElementTypeBase parent, String id, Element def) throws ElementTypeDefinitionException {
        super(bundle, parent, id, def);
        String typeId = stringAttribute(def, "type-id");
        text = stringAttribute(def, "text");
        this.tokenType = bundle.getTokenTypeBundle().getTokenType(typeId);
        setDefaultFormatting(tokenType.getFormatting());

        String flavorName = stringAttribute(def, "flavor");
        if (Strings.isNotEmpty(flavorName)) {
            flavor = TokenTypeCategory.getCategory(flavorName);
        }

        setDescription(tokenType.getValue() + " " + getTokenTypeCategory());
    }

    public TokenElementType(ElementTypeBundle bundle, ElementTypeBase parent, String typeId, String id) {
        super(bundle, parent, id, (String)null);
        tokenType = bundle.getTokenTypeBundle().getTokenType(typeId);
        setDescription(tokenType.getValue() + " " + getTokenTypeCategory());

        setDefaultFormatting(tokenType.getFormatting());
    }

    @Nullable
    public String getText() {
        return text;
    }

    @Override
    public TokenElementTypeLookupCache createLookupCache() {
        return new TokenElementTypeLookupCache(this);
    }

    @NotNull
    @Override
    public TokenElementTypeParser createParser() {
        return new TokenElementTypeParser(this);
    }

    @NotNull
    @Override
    public String getName() {
        return "token (" + getId() + " - " + tokenType.getId() + ")";
    }

    @Override
    public Set<LeafElementType> getNextPossibleLeafs(LanguageNode pathNode, @NotNull ElementLookupContext context) {
        if (isIterationSeparator()) {
            if (parent instanceof IterationElementType) {
                IterationElementType iterationElementType = (IterationElementType) parent;
                ElementTypeLookupCache<?> lookupCache = iterationElementType.iteratedElementType.cache;
                return lookupCache.captureFirstPossibleLeafs(context.reset());
            } else if (parent instanceof QualifiedIdentifierElementType){
                return super.getNextPossibleLeafs(pathNode, context);
            }
        }
        if (parent instanceof WrapperElementType) {
            WrapperElementType wrapperElementType = (WrapperElementType) parent;
            if (this.equals(wrapperElementType.getBeginTokenElement())) {
                ElementTypeLookupCache<?> lookupCache = wrapperElementType.wrappedElement.cache;
                return lookupCache.captureFirstPossibleLeafs(context.reset());
            }
        }

        return super.getNextPossibleLeafs(pathNode, context);
    }

    @Override
    public Set<LeafElementType> getNextRequiredLeafs(LanguageNode pathNode, ParserContext context) {
        if (isIterationSeparator()) {
            if (parent instanceof IterationElementType) {
                IterationElementType iterationElementType = (IterationElementType) parent;
                return iterationElementType.iteratedElementType.cache.getFirstRequiredLeafs();
            } else if (parent instanceof QualifiedIdentifierElementType){
                return super.getNextRequiredLeafs(pathNode, context);
            }
        }
        return super.getNextRequiredLeafs(pathNode, context);
    }

    public boolean isIterationSeparator() {
        return Objects.equals(getId(), SEPARATOR);
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public boolean isIdentifier() {
        return false;
    }

    @Override
    public boolean isSameAs(LeafElementType elementType) {
        if (elementType instanceof TokenElementType) {
            TokenElementType token = (TokenElementType) elementType;
            return token.tokenType == tokenType;
        }
        return false;
    }


    @Override
    public PsiElement createPsiElement(ASTNode astNode) {
        return new TokenPsiElement(astNode, this);
    }

    public String toString() {
        return tokenType.getId() + " (" + getId() + ")";
    }

    public boolean isCharacter() {
        return tokenType.isCharacter();
    }

    @Override
    public TokenLookupItemBuilder getLookupItemBuilder(DBLanguage language) {
        return lookupItemBuilder;
    }

    public TokenTypeCategory getFlavor() {
        return flavor;
    }

    public TokenTypeCategory getTokenTypeCategory() {
        return flavor == null ? tokenType.getCategory() : flavor;
    }
}
