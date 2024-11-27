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

package com.dbn.code.common.lookup;

import com.dbn.code.common.completion.BasicInsertHandler;
import com.dbn.code.common.completion.CodeCompletionContext;
import com.dbn.code.common.completion.options.sorting.CodeCompletionSortingSettings;
import com.dbn.common.util.Naming;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.lookup.LookupItem;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.Objects;

import static com.dbn.common.util.Strings.toLowerCase;
import static com.dbn.common.util.Strings.toUpperCase;


public class CodeCompletionLookupItem extends LookupItem {
    public CodeCompletionLookupItem(LookupItemBuilder lookupItemBuilder, @NotNull String text, CodeCompletionContext completionContext) {
        super(lookupItemBuilder, Naming.unquote(text));
        setIcon(lookupItemBuilder.getIcon());
        if (lookupItemBuilder.isBold()) setBold();
        setAttribute(LookupItem.TYPE_TEXT_ATTR, lookupItemBuilder.getTextHint());
        addLookupStrings(toUpperCase(text), toLowerCase(text));
        setPresentableText(Naming.unquote(text));
        CodeCompletionSortingSettings sortingSettings = completionContext.getCodeCompletionSettings().getSortingSettings();
        if (sortingSettings.isEnabled()) {
            setPriority(sortingSettings.getSortingIndexFor(lookupItemBuilder));
        }
    }

    public CodeCompletionLookupItem(Object source, Icon icon, @NotNull String text, String description, boolean bold, double sortPriority) {
        this(source, icon, text, description, bold);
        setPriority(sortPriority);
    }


    public CodeCompletionLookupItem(Object source, Icon icon, @NotNull String text, String description, boolean bold) {
        super(source, text);
        addLookupStrings(toUpperCase(text), toLowerCase(text));
        setIcon(icon);
        if (bold) setBold();
        setAttribute(LookupItem.TYPE_TEXT_ATTR, description);
        setPresentableText(Naming.unquote(text));
        setInsertHandler(BasicInsertHandler.INSTANCE);
    }

    @NotNull
    @Override
    public Object getObject() {
        return super.getObject();
    }

    @Override
    public InsertHandler getInsertHandler() {
        return super.getInsertHandler();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CodeCompletionLookupItem) {
            CodeCompletionLookupItem lookupItem = (CodeCompletionLookupItem) o;
            return Objects.equals(lookupItem.getLookupString(), getLookupString());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return getLookupString().hashCode();
    }

    @Override
    public boolean isValid() {
        return true;
    }
}
