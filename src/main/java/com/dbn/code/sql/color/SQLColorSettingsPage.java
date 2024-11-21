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

package com.dbn.code.sql.color;

import com.dbn.code.common.color.DBLColorSettingsPage;
import com.dbn.common.icon.Icons;
import com.dbn.language.sql.SQLLanguage;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class SQLColorSettingsPage extends DBLColorSettingsPage {

    public SQLColorSettingsPage() {
        attributeDescriptors.add(new AttributesDescriptor("Line comment", SQLTextAttributesKeys.LINE_COMMENT));
        attributeDescriptors.add(new AttributesDescriptor("Block comment", SQLTextAttributesKeys.BLOCK_COMMENT));
        attributeDescriptors.add(new AttributesDescriptor("String", SQLTextAttributesKeys.STRING));
        attributeDescriptors.add(new AttributesDescriptor("Number", SQLTextAttributesKeys.NUMBER));
        attributeDescriptors.add(new AttributesDescriptor("Alias", SQLTextAttributesKeys.ALIAS));
        attributeDescriptors.add(new AttributesDescriptor("Identifier", SQLTextAttributesKeys.IDENTIFIER));
        attributeDescriptors.add(new AttributesDescriptor("Quoted identifier", SQLTextAttributesKeys.QUOTED_IDENTIFIER));
        attributeDescriptors.add(new AttributesDescriptor("Keyword", SQLTextAttributesKeys.KEYWORD));
        attributeDescriptors.add(new AttributesDescriptor("Function", SQLTextAttributesKeys.FUNCTION));
        attributeDescriptors.add(new AttributesDescriptor("Parameter", SQLTextAttributesKeys.PARAMETER));
        attributeDescriptors.add(new AttributesDescriptor("DataType", SQLTextAttributesKeys.DATA_TYPE));
        attributeDescriptors.add(new AttributesDescriptor("Parenthesis", SQLTextAttributesKeys.PARENTHESIS));
        attributeDescriptors.add(new AttributesDescriptor("Bracket", SQLTextAttributesKeys.BRACKET));
        attributeDescriptors.add(new AttributesDescriptor("Operator", SQLTextAttributesKeys.OPERATOR));
        attributeDescriptors.add(new AttributesDescriptor("Execution variable", SQLTextAttributesKeys.VARIABLE));
        attributeDescriptors.add(new AttributesDescriptor("Procedural block", SQLTextAttributesKeys.CHAMELEON));
    }


    @Override
    @NotNull
    public String getDisplayName() {
        return "SQL (DBN)";
    }

    @Override
    @Nullable
    public Icon getIcon() {
        return Icons.FILE_SQL;
    }

    @Override
    @NotNull
    public SyntaxHighlighter getHighlighter() {
        return SQLLanguage.INSTANCE.getMainLanguageDialect().getSyntaxHighlighter();
    }

    @Override
    public String getDemoTextFileName() {
        return "sql_demo_text.txt";
    }

}
