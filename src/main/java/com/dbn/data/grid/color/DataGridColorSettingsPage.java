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

package com.dbn.data.grid.color;

import com.dbn.common.icon.Icons;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.PlainSyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.dbn.nls.NlsResources.txt;

public class DataGridColorSettingsPage implements ColorSettingsPage {
    protected final List<AttributesDescriptor> attributeDescriptors = new ArrayList<>();
    protected final List<ColorDescriptor> colorDescriptors = new ArrayList<>();

    public DataGridColorSettingsPage() {
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.dataEditor.const.TextAttribute_PLAIN_DATA"), DataGridTextAttributesKeys.PLAIN_DATA));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.dataEditor.const.TextAttribute_AUDIT_DATA"), DataGridTextAttributesKeys.AUDIT_DATA));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.dataEditor.const.TextAttribute_MODIFIED_DATA"), DataGridTextAttributesKeys.MODIFIED_DATA));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.dataEditor.const.TextAttribute_DELETED_DATA"), DataGridTextAttributesKeys.DELETED_DATA));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.dataEditor.const.TextAttribute_ERROR_DATA"), DataGridTextAttributesKeys.ERROR_DATA));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.dataEditor.const.TextAttribute_READONLY_DATA"), DataGridTextAttributesKeys.READONLY_DATA));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.dataEditor.const.TextAttribute_LOADING_DATA"), DataGridTextAttributesKeys.LOADING_DATA));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.dataEditor.const.TextAttribute_PRIMARY_KEY"), DataGridTextAttributesKeys.PRIMARY_KEY));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.dataEditor.const.TextAttribute_FOREIGN_KEY"), DataGridTextAttributesKeys.FOREIGN_KEY));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.dataEditor.const.TextAttribute_SELECTION"), DataGridTextAttributesKeys.SELECTION));
        attributeDescriptors.add(new AttributesDescriptor(txt("cfg.dataEditor.const.TextAttribute_CARET_ROW"), DataGridTextAttributesKeys.CARET_ROW));
    }

    @Override
    public Icon getIcon() {
        return Icons.DBO_TABLE;
    }

    @NotNull
    @Override
    public SyntaxHighlighter getHighlighter() {
        return new PlainSyntaxHighlighter();
    }

    @Override
    @NonNls
    @NotNull
    public final String getDemoText() {
        return " ";
    }

    @Override
    @NotNull
    public AttributesDescriptor[] getAttributeDescriptors() {
        return attributeDescriptors.toArray(new AttributesDescriptor[0]);
    }

    @Override
    @NotNull
    public ColorDescriptor[] getColorDescriptors() {
        return colorDescriptors.toArray(new ColorDescriptor[0]);
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "Data Grid (DBN)";
    }

    @Override
    @Nullable
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }
}
