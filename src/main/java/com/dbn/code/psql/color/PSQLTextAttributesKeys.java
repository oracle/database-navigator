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

package com.dbn.code.psql.color;

import com.intellij.codeInsight.template.impl.TemplateColors;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;

public interface PSQLTextAttributesKeys {
    TextAttributesKey LINE_COMMENT       = TextAttributesKey.createTextAttributesKey("DBN.Attributes.PSQL.LineComment",       DefaultLanguageHighlighterColors.LINE_COMMENT);
    TextAttributesKey BLOCK_COMMENT      = TextAttributesKey.createTextAttributesKey("DBN.Attributes.PSQL.BlockComment",      DefaultLanguageHighlighterColors.BLOCK_COMMENT);
    TextAttributesKey STRING             = TextAttributesKey.createTextAttributesKey("DBN.Attributes.PSQL.String",            DefaultLanguageHighlighterColors.STRING);
    TextAttributesKey NUMBER             = TextAttributesKey.createTextAttributesKey("DBN.Attributes.PSQL.Number",            DefaultLanguageHighlighterColors.NUMBER);
    TextAttributesKey DATA_TYPE          = TextAttributesKey.createTextAttributesKey("DBN.Attributes.PSQL.DataType",          DefaultLanguageHighlighterColors.CONSTANT);
    TextAttributesKey ALIAS              = TextAttributesKey.createTextAttributesKey("DBN.Attributes.PSQL.Alias",             DefaultLanguageHighlighterColors.METADATA);
    TextAttributesKey IDENTIFIER         = TextAttributesKey.createTextAttributesKey("DBN.Attributes.PSQL.Identifier",        DefaultLanguageHighlighterColors.IDENTIFIER);
    TextAttributesKey KEYWORD            = TextAttributesKey.createTextAttributesKey("DBN.Attributes.PSQL.Keyword",           DefaultLanguageHighlighterColors.KEYWORD);
    TextAttributesKey FUNCTION           = TextAttributesKey.createTextAttributesKey("DBN.Attributes.PSQL.Function",          DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL);
    TextAttributesKey PARAMETER          = TextAttributesKey.createTextAttributesKey("DBN.Attributes.PSQL.Parameter",         TemplateColors.TEMPLATE_VARIABLE_ATTRIBUTES);
    TextAttributesKey EXCEPTION          = TextAttributesKey.createTextAttributesKey("DBN.Attributes.PSQL.Exception",         DefaultLanguageHighlighterColors.KEYWORD);
    TextAttributesKey OPERATOR           = TextAttributesKey.createTextAttributesKey("DBN.Attributes.PSQL.Operator",          DefaultLanguageHighlighterColors.OPERATION_SIGN);
    TextAttributesKey PARENTHESIS        = TextAttributesKey.createTextAttributesKey("DBN.Attributes.PSQL.Parenthesis",       DefaultLanguageHighlighterColors.PARENTHESES);
    TextAttributesKey BRACKET            = TextAttributesKey.createTextAttributesKey("DBN.Attributes.PSQL.Brackets",          DefaultLanguageHighlighterColors.BRACKETS);
    TextAttributesKey UNKNOWN_IDENTIFIER = TextAttributesKey.createTextAttributesKey("DBN.Attributes.PSQL.UnknownIdentifier", CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES);
    TextAttributesKey BAD_CHARACTER      = HighlighterColors.BAD_CHARACTER;
}
