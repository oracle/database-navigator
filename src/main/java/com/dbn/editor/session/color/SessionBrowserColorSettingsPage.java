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

package com.dbn.editor.session.color;

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

public class SessionBrowserColorSettingsPage implements ColorSettingsPage {
    protected final List<AttributesDescriptor> attributeDescriptors = new ArrayList<>();
    protected final List<ColorDescriptor> colorDescriptors = new ArrayList<>();

    public SessionBrowserColorSettingsPage() {
        attributeDescriptors.add(new AttributesDescriptor("Active Session",   SessionBrowserTextAttributesKeys.ACTIVE_SESSION));
        attributeDescriptors.add(new AttributesDescriptor("Inactive Session", SessionBrowserTextAttributesKeys.INACTIVE_SESSION));
        attributeDescriptors.add(new AttributesDescriptor("Cached Session",   SessionBrowserTextAttributesKeys.CACHED_SESSION));
        attributeDescriptors.add(new AttributesDescriptor("Sniped Session",   SessionBrowserTextAttributesKeys.SNIPED_SESSION));
        attributeDescriptors.add(new AttributesDescriptor("Killed Session",   SessionBrowserTextAttributesKeys.KILLED_SESSION));
    }

    @Override
    public Icon getIcon() {
        return Icons.FILE_SESSION_BROWSER;
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
        return "Session Browser (DBN)";
    }

    @Override
    @Nullable
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }
}
