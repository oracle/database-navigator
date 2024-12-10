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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Getter
public class TokenTypeBundle extends TokenTypeBundleBase {
    private final DBLanguage baseLanguage;

    private final IElementType integer;
    private final IElementType number;
    private final IElementType string;
    private final IElementType operator;
    private final IElementType keyword;
    private final IElementType function;
    private final IElementType variable;
    private final IElementType parameter;
    private final IElementType exception;
    private final IElementType dataType;

    private static final Set<String> GENERIC_TOKENS = new HashSet<>(Arrays.asList("INTEGER", "NUMBER", "STRING", "OPERATOR", "KEYWORD", "FUNCTION", "VARIABLE", "PARAMETER", "EXCEPTION", "DATA_TYPE"));

    public TokenTypeBundle(DBLanguageDialect languageDialect, Document document) {
        super(languageDialect, document);
        this.baseLanguage = languageDialect.getBaseLanguage();
        initIndex(getSharedTokenTypes().size());

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

    public TokenType getTokenType(short index) {
        TokenType tokenType = super.getTokenType(index);
        if (tokenType == null ){
            return getSharedTokenTypes().getTokenType(index);
        }
        return tokenType;
    }

    public SharedTokenTypeBundle getSharedTokenTypes() {
        return baseLanguage.getSharedTokenTypes();
    }

    public DBLanguageDialect getLanguageDialect() {
        return (DBLanguageDialect) getLanguage();
    }

    @Override
    public SimpleTokenType getCharacterTokenType(int index) {
        return getSharedTokenTypes().getCharacterTokenType(index);
    }

    @Override
    public SimpleTokenType getOperatorTokenType(int index) {
        return getSharedTokenTypes().getOperatorTokenType(index);
    }

    @Override
    public SimpleTokenType getTokenType(String id) {
        SimpleTokenType tokenType = super.getTokenType(id);
        if (tokenType != null) return tokenType;

        tokenType = getSharedTokenTypes().getTokenType(id);
        if (tokenType != null) return tokenType;


        if (!GENERIC_TOKENS.contains(id)) {
            log.warn("DBN - [{}] undefined token type: {}", getLanguage().getID(), id);
        }
        //log.info("[DBN-WARNING] Undefined token type: " + id);
        return getSharedTokenTypes().getIdentifier();
    }

    @Override
    public TokenSet getTokenSet(String id) {
        TokenSet tokenSet = super.getTokenSet(id);
        if (tokenSet != null) return tokenSet;

        tokenSet = getSharedTokenTypes().getTokenSet(id);
        if (tokenSet != null) return tokenSet;


        log.warn("DBN - [{}] undefined token set: {}", getLanguage().getID(), id);

        tokenSet = super.getTokenSet("UNDEFINED");
        return tokenSet;
    }

    public SimpleTokenType getIdentifier() {
        return getSharedTokenTypes().getIdentifier();
    }

    public SimpleTokenType getVariable() {
        return getSharedTokenTypes().getVariable();
    }

    public SimpleTokenType getString() {
        return getSharedTokenTypes().getString();
    }


    public ChameleonElementType getChameleon(DBLanguageDialectIdentifier dialectIdentifier) {
        return getLanguageDialect().getChameleonTokenType(dialectIdentifier);
    }
}
