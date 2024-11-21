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

package com.dbn.code.common.completion.options.filter;

import com.dbn.code.common.completion.options.CodeCompletionSettings;
import com.dbn.code.common.completion.options.filter.ui.CodeCompletionFiltersSettingsForm;
import com.dbn.common.options.CompositeConfiguration;
import com.dbn.common.options.Configuration;
import com.dbn.language.common.TokenTypeCategory;
import com.dbn.object.type.DBObjectType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = false)
public class CodeCompletionFiltersSettings extends CompositeConfiguration<CodeCompletionSettings, CodeCompletionFiltersSettingsForm> {
    private final @Getter(lazy = true) CodeCompletionFilterSettings basicFilterSettings = new CodeCompletionFilterSettings(this, false);
    private final @Getter(lazy = true) CodeCompletionFilterSettings extendedFilterSettings = new CodeCompletionFilterSettings(this, true);

    public CodeCompletionFiltersSettings(CodeCompletionSettings parent) {
        super(parent);
    }

    @Override
    public String getDisplayName() {
        return txt("cfg.codeCompletion.title.Filters");
    }

   /*********************************************************
    *                         Custom                        *
    *********************************************************/
    public CodeCompletionFilterSettings getFilterSettings(boolean extended) {
        return extended ? getExtendedFilterSettings() : getBasicFilterSettings();
    }

    boolean acceptRootObjects(boolean extended, DBObjectType objectType) {
        return getFilterSettings(extended).acceptsRootObject(objectType);
    }

    boolean showReservedWords(boolean extended, TokenTypeCategory tokenTypeCategory) {
        return getFilterSettings(extended).acceptReservedWord(tokenTypeCategory);
    }

    boolean showUserSchemaObjects(boolean extended, DBObjectType objectType) {
        return getFilterSettings(extended).acceptsCurrentSchemaObject(objectType);
    }

    boolean acceptPublicSchemaObjects(boolean extended, DBObjectType objectType) {
        return getFilterSettings(extended).acceptsPublicSchemaObject(objectType);
    }

    boolean acceptAnySchemaObjects(boolean extended, DBObjectType objectType) {
        return getFilterSettings(extended).acceptsAnySchemaObject(objectType);
    }

    /*********************************************************
    *                   Configuration                       *
    *********************************************************/
    @Override
    @NotNull
    public CodeCompletionFiltersSettingsForm createConfigurationEditor() {
        return new CodeCompletionFiltersSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "filters";
    }

    @Override
    protected Configuration[] createConfigurations() {
        return new Configuration[] {
                getBasicFilterSettings(),
                getExtendedFilterSettings()};
    }
}