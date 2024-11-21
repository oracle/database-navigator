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

package com.dbn.code.common.completion.options.sorting;

import com.dbn.common.options.BasicConfiguration;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.language.common.TokenTypeCategory;
import com.dbn.object.type.DBObjectType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.dbn.common.options.setting.Settings.stringAttribute;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class CodeCompletionSortingItem extends BasicConfiguration<CodeCompletionSortingSettings, ConfigurationEditorForm> {
    private DBObjectType objectType;
    private TokenTypeCategory tokenTypeCategory = TokenTypeCategory.UNKNOWN;

    CodeCompletionSortingItem(CodeCompletionSortingSettings parent) {
        super(parent);
    }

    public DBObjectType getObjectType() {
        return objectType;
    }

    public TokenTypeCategory getTokenTypeCategory() {
        return tokenTypeCategory;
    }

    public String getTokenTypeName() {
        return tokenTypeCategory.getName();
    }

    public String toString() {
        return objectType == null ? tokenTypeCategory.getName() : objectType.getName();
    }

    /*********************************************************
     *                      Configuration                    *
     *********************************************************/
    @Override
    @NotNull
    public ConfigurationEditorForm createConfigurationEditor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getConfigElementName() {
        return "sorting-element";
    }

    @Override
    public void readConfiguration(Element element) {
        String sortingItemType = stringAttribute(element, "type");
        if (Objects.equals(sortingItemType, "OBJECT")) {
            String objectTypeName = stringAttribute(element, "id");
            objectType = DBObjectType.get(objectTypeName);
        } else {
            String tokenTypeName = stringAttribute(element, "id");
            tokenTypeCategory = TokenTypeCategory.getCategory(tokenTypeName);
        }
    }

    @Override
    public void writeConfiguration(Element element) {
        if (objectType != null) {
            element.setAttribute("type", "OBJECT");
            element.setAttribute("id", objectType.getName());
        } else {
            element.setAttribute("type", "RESERVED_WORD");
            element.setAttribute("id", tokenTypeCategory.getName());
        }
    }
}
