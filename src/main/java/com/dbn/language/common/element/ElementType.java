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

package com.dbn.language.common.element;

import com.dbn.code.common.style.formatting.FormattingDefinition;
import com.dbn.common.property.PropertyHolder;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.impl.LeafElementType;
import com.dbn.language.common.element.path.LanguageNodeBase;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NonNls;

import javax.swing.Icon;
import java.util.Set;

public interface ElementType extends PropertyHolder<ElementTypeAttribute>{

    @NonNls
    String getId();

    @NonNls
    String getName();

    @NonNls
    default String getDebugName() {
        return getName();
    }

    @NonNls
    String getDescription();

    Icon getIcon();

    DBLanguage getLanguage();

    DBLanguageDialect getLanguageDialect();

    boolean isLeaf();

    boolean isVirtualObject();

    PsiElement createPsiElement(ASTNode astNode);

    FormattingDefinition getFormatting();

    void setDefaultFormatting(FormattingDefinition defaults);

    boolean isWrappingBegin(LeafElementType elementType);

    boolean isWrappingBegin(TokenType tokenType);

    boolean isWrappingEnd(LeafElementType elementType);

    boolean isWrappingEnd(TokenType tokenType);

    int getIndexInParent(LanguageNodeBase node);

    TokenType getTokenType();

    default void collectLeafElements(Set<LeafElementType> leafElementTypes) {};
}
