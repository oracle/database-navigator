package com.dbn.language.common.element.impl;

import com.dbn.code.common.style.formatting.FormattingDefinition;
import com.dbn.common.latent.Latent;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.cache.IterationElementTypeLookupCache;
import com.dbn.language.common.element.parser.impl.IterationElementTypeParser;
import com.dbn.language.common.element.util.ElementTypeDefinitionException;
import com.dbn.language.common.psi.SequencePsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import static com.dbn.common.options.setting.Settings.stringAttribute;

public final class IterationElementType extends ElementTypeBase {

    public ElementTypeBase iteratedElementType;
    public TokenElementType[] separatorTokens;
    public int[] elementsCountVariants;
    public int minIterations;

    private final Latent<Boolean> followedBySeparator = Latent.basic(() -> {
        if (separatorTokens != null) {
            for (TokenElementType separatorToken : separatorTokens) {
                if (cache.isNextPossibleToken(separatorToken.tokenType)) {
                    return Boolean.TRUE;
                }
            }
        }
        return Boolean.FALSE;
    });

    public IterationElementType(ElementTypeBundle bundle, ElementTypeBase parent, String id, Element def) throws ElementTypeDefinitionException {
        super(bundle, parent, id, def);
    }

    @Override
    protected IterationElementTypeLookupCache createLookupCache() {
        return new IterationElementTypeLookupCache(this);
    }

    @NotNull
    @Override
    protected IterationElementTypeParser createParser() {
        return new IterationElementTypeParser(this);
    }

    @Override
    protected void loadDefinition(Element def) throws ElementTypeDefinitionException {
        super.loadDefinition(def);
        String separatorTokenIds = stringAttribute(def, "separator");
        if (separatorTokenIds != null) {
            StringTokenizer tokenizer = new StringTokenizer(separatorTokenIds, ",");
            List<TokenElementType> separators = new ArrayList<>();
            while (tokenizer.hasMoreTokens()) {
                String separatorTokenId = tokenizer.nextToken().trim();
                TokenElementType separatorToken = new TokenElementType(bundle, this, separatorTokenId, TokenElementType.SEPARATOR);
                        //bundle.getTokenElementType(separatorTokenId);
                separatorToken.setDefaultFormatting(separatorToken.isCharacter() ?
                        FormattingDefinition.NO_SPACE_BEFORE :
                        FormattingDefinition.ONE_SPACE_BEFORE);
                separators.add(separatorToken);
            }
            separatorTokens = separators.toArray(new TokenElementType[0]);
        }

        List<Element> children = def.getChildren();
        if (children.size() != 1) {
            throw new ElementTypeDefinitionException("[" + getLanguageDialect().getID() + "] Invalid iteration definition (id=" + getId() + "). Element should contain exactly one child.");
        }
        Element child = children.get(0);
        String type = child.getName();
        iteratedElementType = bundle.resolveElementDefinition(child, type, this);

        String elementsCountDef = stringAttribute(def, "elements-count");
        if (elementsCountDef != null) {
            List<Integer> variants = new ArrayList<>();
            StringTokenizer tokenizer = new StringTokenizer(elementsCountDef, ",");
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                int index = token.indexOf('-');
                if (index > -1) {
                    int start = Integer.parseInt(token.substring(0, index).trim());
                    int end  = Integer.parseInt(token.substring(index + 1).trim());
                    for (int i=start; i<=end; i++) {
                        variants.add(i);
                    }
                } else {
                    variants.add(Integer.parseInt(token.trim()));
                }
            }

            elementsCountVariants = new int[variants.size()];
            for (int i=0; i< elementsCountVariants.length; i++) {
                elementsCountVariants[i] = variants.get(i);
            }
        }

        String minIterationsDef = stringAttribute(def, "min-iterations");
        if (minIterationsDef != null) {
            minIterations = Integer.parseInt(minIterationsDef);
        }
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @NotNull
    @Override
    public String getName() {
        return "iteration (" + getId() + ")";
    }

    @Override
    public PsiElement createPsiElement(ASTNode astNode) {
        return new SequencePsiElement(astNode, this);
    }

    public boolean isSeparator(TokenElementType tokenElementType) {
        if (separatorTokens != null) {
            for (TokenElementType separatorToken: separatorTokens) {
                if (separatorToken == tokenElementType) return true;
            }
        }
        return false;
    }

    public boolean isSeparator(TokenType tokenType) {
        if (separatorTokens != null) {
            for (TokenElementType separatorToken: separatorTokens) {
                if (separatorToken.tokenType == tokenType) return true;
            }
        }
        return false;
    }

    public boolean isFollowedBySeparator() {
        return followedBySeparator.get() == Boolean.TRUE;
    }
}
