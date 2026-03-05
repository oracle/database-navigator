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

import com.dbn.common.util.XmlContents;
import com.intellij.psi.tree.TokenSet;
import lombok.SneakyThrows;
import org.jdom.Document;

import java.util.HashSet;
import java.util.Set;

public class SharedTokenTypeBundle extends TokenTypeBundleBase {
    public final SimpleTokenType whiteSpace;
    public final SimpleTokenType identifier;
    public final SimpleTokenType quotedIdentifier;
    public final SimpleTokenType variable;
    public final SimpleTokenType string;
    public final SimpleTokenType number;
    public final SimpleTokenType integer;
    public final SimpleTokenType lineComment;
    public final SimpleTokenType blockComment;

    public final SimpleTokenType chrLeftParenthesis;
    public final SimpleTokenType chrRightParenthesis;
    public final SimpleTokenType chrLeftBracket;
    public final SimpleTokenType chrRightBracket;
    public final SimpleTokenType chrLeftBrace;
    public final SimpleTokenType chrRightBrace;

    public final SimpleTokenType chrDot;
    public final SimpleTokenType chrComma;
    public final SimpleTokenType chrColon;
    public final SimpleTokenType chrSemicolon;
    public final SimpleTokenType chrSlash;
    public final SimpleTokenType chrStar;

    public final TokenSet whitespaceTokens;
    public final TokenSet commentTokens;
    public final TokenSet stringTokens;

    public final Set<TokenType> identifierTokens;

    public SharedTokenTypeBundle(DBLanguage language) {
        super(language, loadDefinition());
        whiteSpace = getTokenType("WHITE_SPACE");
        identifier = getTokenType("IDENTIFIER");
        quotedIdentifier = getTokenType("QUOTED_IDENTIFIER");
        variable = getTokenType("VARIABLE");
        string = getTokenType("STRING");
        number = getTokenType("NUMBER");
        integer = getTokenType("INTEGER");
        lineComment = getTokenType("LINE_COMMENT");
        blockComment = getTokenType("BLOCK_COMMENT");


        chrLeftParenthesis = getTokenType("CHR_LEFT_PARENTHESIS");
        chrRightParenthesis = getTokenType("CHR_RIGHT_PARENTHESIS");
        chrLeftBracket = getTokenType("CHR_LEFT_BRACKET");
        chrRightBracket = getTokenType("CHR_RIGHT_BRACKET");
        chrLeftBrace = getTokenType("CHR_LEFT_BRACE");
        chrRightBrace = getTokenType("CHR_RIGHT_BRACE");

        chrDot = getTokenType("CHR_DOT");
        chrComma = getTokenType("CHR_COMMA");
        chrColon = getTokenType("CHR_COLON");
        chrSemicolon = getTokenType("CHR_SEMICOLON");
        chrSlash = getTokenType("CHR_SLASH");
        chrStar = getTokenType("CHR_STAR");

        whitespaceTokens = getTokenSet("WHITE_SPACES");
        commentTokens = getTokenSet("COMMENTS");
        stringTokens = getTokenSet("STRINGS");

        identifierTokens = new HashSet<>(2);
        identifierTokens.add(identifier);
        identifierTokens.add(quotedIdentifier);
    }

    @SneakyThrows
    private static Document loadDefinition() {
        return XmlContents.fileToDocument(SharedTokenTypeBundle.class, "db_language_common_tokens.xml");
    }


    public boolean isIdentifier(TokenType tokenType) {
        return tokenType == identifier || tokenType == quotedIdentifier;
    }

    public boolean isVariable(TokenType tokenType) {
        return tokenType == variable;
    }
}
