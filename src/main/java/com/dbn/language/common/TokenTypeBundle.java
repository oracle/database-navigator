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

package com.dbn.language.common;

import com.dbn.language.common.element.ChameleonElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Document;
import org.jetbrains.annotations.NonNls;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@NonNls
@Getter
public class TokenTypeBundle extends TokenTypeBundleBase {
    private final DBLanguage baseLanguage;
    private final SharedTokenTypeBundle sharedTokenTypes;

    public final IElementType integer;
    public final IElementType number;
    public final IElementType string;
    public final IElementType operator;
    public final IElementType keyword;
    public final IElementType function;
    public final IElementType variable;
    public final IElementType parameter;
    public final IElementType exception;
    public final IElementType dataType;

    public static final Set<String> GENERIC_TOKENS = Set.of("INTEGER", "NUMBER", "STRING", "OPERATOR", "KEYWORD", "FUNCTION", "VARIABLE", "PARAMETER", "EXCEPTION", "DATA_TYPE");
    private final Set<String> undefinedTokens = new HashSet<>();

    public TokenTypeBundle(DBLanguageDialect languageDialect, Document document) {
        super(languageDialect, document);
        this.baseLanguage = languageDialect.getBaseLanguage();
        this.sharedTokenTypes = languageDialect.getSharedTokenTypes();

        this.integer   = getTokenType("INTEGER");
        this.number    = getTokenType("NUMBER");
        this.string    = getTokenType("STRING");
        this.operator  = getTokenType("OPERATOR");
        this.keyword   = getTokenType("KEYWORD");
        this.function  = getTokenType("FUNCTION");
        this.variable  = getTokenType("VARIABLE");
        this.parameter = getTokenType("PARAMETER");
        this.exception = getTokenType("EXCEPTION");
        this.dataType  = getTokenType("DATA_TYPE");
    }

    @Override
    protected int getInitialIndex() {
        DBLanguage baseLanguage = getLanguageDialect().getBaseLanguage();
        return baseLanguage.getSharedTokenTypes().size();
    }

    public TokenType getTokenType(int index) {
        TokenType tokenType = super.getTokenType(index);
        if (tokenType == null) {
            return sharedTokenTypes.getTokenType(index);
        }
        return tokenType;
    }

    public DBLanguageDialect getLanguageDialect() {
        return (DBLanguageDialect) getLanguage();
    }

    @Override
    public SimpleTokenType getCharacterTokenType(int index) {
        return sharedTokenTypes.getCharacterTokenType(index);
    }

    @Override
    public SimpleTokenType getOperatorTokenType(int index) {
        return sharedTokenTypes.getOperatorTokenType(index);
    }

    @Override
    public SimpleTokenType getTokenType(@NonNls String id) {
        SimpleTokenType tokenType = super.getTokenType(id);
        if (tokenType != null) return tokenType;

        tokenType = sharedTokenTypes.getTokenType(id);
        if (tokenType != null) return tokenType;


        if (!GENERIC_TOKENS.contains(id) && !undefinedTokens.contains(id)) {
            undefinedTokens.add(id);
            log.warn("DBN - [{}] undefined token type: {}", getLanguage().getID(), id);
        }
        //log.info("[DBN-WARNING] Undefined token type: " + id);
        return sharedTokenTypes.identifier;
    }

    @Override
    public TokenSet getTokenSet(@NonNls String id) {
        TokenSet tokenSet = super.getTokenSet(id);
        if (tokenSet != null) return tokenSet;

        tokenSet = sharedTokenTypes.getTokenSet(id);
        if (tokenSet != null) return tokenSet;


        log.warn("DBN - [{}] undefined token set: {}", getLanguage().getID(), id);

        tokenSet = super.getTokenSet("UNDEFINED");
        return tokenSet;
    }

    public SimpleTokenType getIdentifier() {
        return sharedTokenTypes.identifier;
    }

    public SimpleTokenType getVariable() {
        return sharedTokenTypes.variable;
    }

    public SimpleTokenType getString() {
        return sharedTokenTypes.string;
    }


    public ChameleonElementType getChameleon(DBLanguageDialectIdentifier dialectIdentifier) {
        return getLanguageDialect().getChameleonTokenType(dialectIdentifier);
    }
}
