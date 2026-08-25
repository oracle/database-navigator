/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.language.common.element.impl;

import com.dbn.code.common.style.formatting.FormattingDefinition;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.cache.IterationElementTypeCache;
import com.dbn.language.common.element.parser.impl.IterationElementTypeParser;
import com.dbn.language.common.element.util.ElementTypeDefinitionException;
import com.dbn.language.common.psi.SequencePsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.language.common.element.util.ElementTypeAttribute.ITERATION_SEPARATOR;

@Slf4j
public class IterationElementType extends ElementTypeBase {

    public ElementTypeBase iteratedElement;
    public TokenElementType[] separatorTokens;
    public int[] elementsCountVariants;
    public int minIterations;

    private Boolean followedBySeparator;

    public IterationElementType(ElementTypeBundle bundle, ElementTypeBase parent, String id, Element def) throws ElementTypeDefinitionException {
        super(bundle, parent, id, def);
    }

    @Override
    protected IterationElementTypeCache createLookupCache() {
        return new IterationElementTypeCache(this);
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
        initSeparatorTokens(separatorTokenIds);

        List<Element> children = def.getChildren();
        if (children.size() != 1) {
            throw new ElementTypeDefinitionException("[" + getLanguageDialect().getID() + "] Invalid iteration definition (id=" + getId() + "). Element should contain exactly one child.");
        }
        Element child = children.get(0);
        String type = child.getName();
        if (isMarkedOptional(child)) {
            // not supported - prevent false expectations
            log.warn("DBN - [{}] iterated element cannot be optional (iteration = {})", getLanguageDialect().getID(), getId());
        }

        iteratedElement = bundle.resolveElementDefinition(child, type, this);

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

    public void initSeparatorTokens(String separatorTokenIds) {
        if (separatorTokenIds == null) return;

        StringTokenizer tokenizer = new StringTokenizer(separatorTokenIds, ",");
        List<TokenElementType> separators = new ArrayList<>();
        while (tokenizer.hasMoreTokens()) {
            String separatorTokenId = tokenizer.nextToken().trim();
            TokenElementType separatorToken = new TokenElementType(this, separatorTokenId, id + ".i");
                    //bundle.getTokenElementType(separatorTokenId);

            separatorToken.set(ITERATION_SEPARATOR, true);
            separatorToken.setDefaultFormatting(separatorToken.isCharacter() ?
                    FormattingDefinition.NO_SPACE_BEFORE :
                    FormattingDefinition.ONE_SPACE_BEFORE);
            separators.add(separatorToken);
        }
        separatorTokens = separators.toArray(new TokenElementType[0]);
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
        if (separatorTokens == null) return false;

        for (TokenElementType separatorToken: separatorTokens) {
            if (separatorToken == tokenElementType) return true;
        }
        return false;
    }

    public boolean isSeparator(TokenType tokenType) {
        if (separatorTokens == null) return false;

        for (TokenElementType separatorToken: separatorTokens) {
            if (separatorToken.tokenType == tokenType) return true;
        }
        return false;
    }

    public boolean isFollowedBySeparator() {
        if (followedBySeparator == null) {
            followedBySeparator = evaluateFollowedBySeparator();
        }
        return followedBySeparator;
    }

    private boolean evaluateFollowedBySeparator() {
        if (separatorTokens == null) return false;

        for (TokenElementType separatorToken : separatorTokens) {
            if (cache.isNextPossibleToken(separatorToken.tokenType)) {
                return true;
            }
        }
        return false;
    }
}
