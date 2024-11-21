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

package com.dbn.code.common.color;

import com.dbn.common.util.Commons;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
public abstract class DBLColorSettingsPage implements ColorSettingsPage {

    private String demoText;
    protected final List<AttributesDescriptor> attributeDescriptors = new ArrayList<>();

    @Override
    @NonNls
    @NotNull
    public final String getDemoText() {
        if (demoText == null) {
            String demoTextFileName = getDemoTextFileName();
            try (InputStream inputStream = getClass().getResourceAsStream(demoTextFileName)) {
                demoText = Commons.readInputStream(inputStream);
            } catch (IOException e) {
                conditionallyLog(e);
                log.error("Failed to load file " + demoTextFileName, e);
            }
        }
        return demoText.replace("\r\n", "\n");
    }

    public abstract String getDemoTextFileName();

    @Override
    @NotNull
    public AttributesDescriptor[] getAttributeDescriptors() {
        return attributeDescriptors.toArray(new AttributesDescriptor[0]);
    }

    @Override
    @NotNull
    public ColorDescriptor[] getColorDescriptors() {
        return new ColorDescriptor[0];
    }

    @Override
    @Nullable
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }
}
