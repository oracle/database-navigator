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

import com.dbn.code.common.style.formatting.FormattingDefinition;
import com.dbn.common.index.Indexable;
import com.dbn.language.common.element.TokenPairTemplate;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface TokenType extends Indexable {

    String getId();

    int getLookupIndex();

    String getValue();

    String getDescription();

    String getTypeName();

    boolean isSuppressibleReservedWord();

    boolean isIdentifier();

    boolean isVariable();

    boolean isQuotedIdentifier();

    boolean isKeyword();

    boolean isFunction();

    boolean isParameter();

    boolean isDataType();

    boolean isLiteral();

    boolean isNumeric();

    boolean isCharacter();

    boolean isOperator();

    boolean isChameleon();

    boolean isReservedWord();

    boolean isParserLandmark();

    @NotNull
    TokenTypeCategory getCategory();

    TokenTypeBundleBase getBundle();

    @Nullable
    DBObjectType getObjectType();

    FormattingDefinition getFormatting();

    TokenPairTemplate getTokenPairTemplate();

    void setDefaultFormatting(FormattingDefinition defaults);

    boolean isOneOf(TokenType ... tokenTypes);

    boolean matches(TokenType tokenType);
}
