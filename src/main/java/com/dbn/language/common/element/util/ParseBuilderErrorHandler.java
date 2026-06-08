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

package com.dbn.language.common.element.util;

import com.dbn.common.util.Strings;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.TokenTypeCategory;
import com.dbn.language.common.element.parser.ParserBuilder;
import com.dbn.language.common.element.parser.ParserContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dbn.common.util.Strings.toUpperCase;
import static com.dbn.nls.NlsResources.txt;

public class ParseBuilderErrorHandler {
    public static void updateBuilderError(Set<TokenType> expectedTokens, ParserContext context) {
        if (expectedTokens != null) {
            ParserBuilder builder = context.builder;
            if (!builder.isErrorAtOffset()) {
                //expectedTokenError(1, context.builder, TokenTypeCategory.CHARACTER, expectedTokens);
                //expectedTokenError(2, context.builder, TokenTypeCategory.OPERATOR, expectedTokens);
                //expectedTokenError(3, context.builder, TokenTypeCategory.KEYWORD, expectedTokens);
                //expectedTokenError(4, context.builder, TokenTypeCategory.FUNCTION, expectedTokens);
                //expectedTokenError(5, context.builder, TokenTypeCategory.DATATYPE, expectedTokens);
                //expectedTokenError(6, context.builder, TokenTypeCategory.IDENTIFIER, expectedTokens);

                Set<String> tokenDescriptions = new HashSet<>(expectedTokens.size());
                for (TokenType tokenType : expectedTokens) {
                    if (tokenType.isFunction()) {
                        tokenDescriptions.add(txt("msg.languageParser.token.Type_FUNCTION"));
                        continue;
                    }
                    String value = tokenType.getValue();
                    String description =
                            tokenType.isIdentifier() ? txt("msg.languageParser.token.Type_IDENTIFIER") :
                                    Strings.isNotEmptyOrSpaces(value) ? toUpperCase(value) : tokenType.getTypeName();

                    tokenDescriptions.add(description);
                }

                String[] tokenDesc = tokenDescriptions.toArray(new String[0]);
                Arrays.sort(tokenDesc);

                String tokens = String.join("\n", tokenDesc);
                String message = tokenDesc.length > 1 ?
                        txt("msg.languageParser.text.ExpectedTokenOptions", tokens) :
                        txt("msg.languageParser.text.ExpectedToken", tokens);
                //buffer.append("\n");
                builder.markError(txt("msg.languageParser.error.InvalidOrIncompleteStatement"));
                builder.error(message);
            }
        }
    }

    private static void expectedTokenError(int index, ParserBuilder builder, TokenTypeCategory category, Set<TokenType> expectedTokens) {
        Set<TokenType> tokenTypes = expectedTokens
                .stream()
                .filter(tokenType -> tokenType.getCategory() == category)
                .collect(Collectors.toSet());

        if (!tokenTypes.isEmpty()) {
            String message;
            switch (category) {
                case IDENTIFIER: {
                    message = txt("msg.languageParser.token.Type_IDENTIFIER");
                    break;
                }
                case CHARACTER:
                case OPERATOR: {
                    message = txt("msg.languageParser.text.ExpectedCategoryExamples", getCategoryName(category), tokenTypes
                            .stream()
                            .map(tokenType -> tokenType.getId().substring(4).replace("_", " "))
                            .distinct()
                            .sorted()
                            .collect(Collectors.joining(", ")));
                    break;
                }
                default: {
                    message = txt("msg.languageParser.text.ExpectedCategoryExamplesMore", getCategoryName(category),
                            tokenTypes
                                    .stream()
                                    .map(tokenType -> toUpperCase(tokenType.getValue()))
                                    .distinct()
                                    .limit(20)
                                    .sorted()
                                    .collect(Collectors.joining(", ")));
                }
            }
            builder.markError(message);
        }

    }

    private static String getCategoryName(TokenTypeCategory category) {
        return switch (category) {
            case CHARACTER -> txt("msg.languageParser.token.Category_CHARACTER");
            case CHAMELEON -> txt("msg.languageParser.token.Category_CHAMELEON");
            case COMMENT -> txt("msg.languageParser.token.Category_COMMENT");
            case DATATYPE -> txt("msg.languageParser.token.Category_DATATYPE");
            case EXCEPTION -> txt("msg.languageParser.token.Category_EXCEPTION");
            case FUNCTION -> txt("msg.languageParser.token.Category_FUNCTION");
            case IDENTIFIER -> txt("msg.languageParser.token.Category_IDENTIFIER");
            case KEYWORD -> txt("msg.languageParser.token.Category_KEYWORD");
            case LITERAL -> txt("msg.languageParser.token.Category_LITERAL");
            case NUMERIC -> txt("msg.languageParser.token.Category_NUMERIC");
            case OBJECT -> txt("msg.languageParser.token.Category_OBJECT");
            case OPERATOR -> txt("msg.languageParser.token.Category_OPERATOR");
            case PARAMETER -> txt("msg.languageParser.token.Category_PARAMETER");
            case VARIABLE -> txt("msg.languageParser.token.Category_VARIABLE");
            case WHITESPACE -> txt("msg.languageParser.token.Category_WHITESPACE");
            default -> txt("msg.languageParser.token.Category_UNKNOWN");
        };
    }
}
