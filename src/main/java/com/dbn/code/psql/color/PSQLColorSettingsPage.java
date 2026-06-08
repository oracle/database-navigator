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

import com.dbn.code.common.color.DBLColorSettingsPage;
import com.dbn.common.icon.Icons;
import com.dbn.language.psql.PSQLLanguage;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import static com.dbn.nls.NlsResources.txt;

public class PSQLColorSettingsPage extends DBLColorSettingsPage {
    public PSQLColorSettingsPage() {
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_LINE_COMMENT"), PSQLTextAttributesKeys.LINE_COMMENT));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_BLOCK_COMMENT"), PSQLTextAttributesKeys.BLOCK_COMMENT));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_STRING_LITERAL"), PSQLTextAttributesKeys.STRING));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_NUMERIC_LITERAL"), PSQLTextAttributesKeys.NUMBER));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_ALIAS"), PSQLTextAttributesKeys.ALIAS));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_IDENTIFIER"), PSQLTextAttributesKeys.IDENTIFIER));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_KEYWORD"), PSQLTextAttributesKeys.KEYWORD));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_FUNCTION"), PSQLTextAttributesKeys.FUNCTION));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_DATA_TYPE"), PSQLTextAttributesKeys.DATA_TYPE));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_PARENTHESIS"), PSQLTextAttributesKeys.PARENTHESIS));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_EXCEPTION"), PSQLTextAttributesKeys.EXCEPTION));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_BRACKET"), PSQLTextAttributesKeys.BRACKET));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.codeEditor.const.TextAttribute_OPERATOR"), PSQLTextAttributesKeys.OPERATOR));
    }

    @Override
    @NotNull
    public String getDisplayName() {
        return txt("cfg.codeEditor.title.PlSqlColorSettings");
    }
    @Override
    @Nullable
    public Icon getIcon() {
        return Icons.FILE_PLSQL;
    }

    @Override
    @NotNull
    public SyntaxHighlighter getHighlighter() {
        return PSQLLanguage.INSTANCE.getMainLanguageDialect().getSyntaxHighlighter();
    }

    @Override
    public String getDemoTextFileName() {
        return "plsql_demo_text.txt";
    }
}
