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

import static com.dbn.nls.NlsResources.txt;

public class SQLColorSettingsPage extends DBLColorSettingsPage {

    public SQLColorSettingsPage() {
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_LINE_COMMENT"), SQLTextAttributesKeys.LINE_COMMENT));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_BLOCK_COMMENT"), SQLTextAttributesKeys.BLOCK_COMMENT));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_STRING"), SQLTextAttributesKeys.STRING));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_NUMBER"), SQLTextAttributesKeys.NUMBER));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_ALIAS"), SQLTextAttributesKeys.ALIAS));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_IDENTIFIER"), SQLTextAttributesKeys.IDENTIFIER));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_KEYWORD"), SQLTextAttributesKeys.KEYWORD));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_FUNCTION"), SQLTextAttributesKeys.FUNCTION));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_PARAMETER"), SQLTextAttributesKeys.PARAMETER));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_DATA_TYPE"), SQLTextAttributesKeys.DATA_TYPE));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_PARENTHESIS"), SQLTextAttributesKeys.PARENTHESIS));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_BRACKET"), SQLTextAttributesKeys.BRACKET));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_OPERATOR"), SQLTextAttributesKeys.OPERATOR));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_EXECUTION_VARIABLE"), SQLTextAttributesKeys.VARIABLE));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_PROCEDURAL_BLOCK"), SQLTextAttributesKeys.CHAMELEON));
    }


    @Override
    @NotNull
    public String getDisplayName() {
        return txt("cfg.codeEditor.title.SqlColorSettings");
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
