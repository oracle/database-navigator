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

import com.dbn.code.common.completion.CodeCompletionContext;
import com.dbn.code.common.completion.CodeCompletionLookupConsumer;
import com.dbn.code.common.completion.options.sorting.CodeCompletionSortingSettings;
import com.dbn.common.ref.WeakRefCache;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.psql.PSQLLanguage;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.object.common.DBObject;

import javax.swing.Icon;

public abstract class LookupItemBuilder {
    private static final WeakRefCache<DBObject, LookupItemBuilder> sqlCache = WeakRefCache.weakKey();
    private static final WeakRefCache<DBObject, LookupItemBuilder> psqlCache = WeakRefCache.weakKey();


    public static LookupItemBuilder of(DBObject object, DBLanguage<?> language) {
        if (language == SQLLanguage.INSTANCE) {
            return sqlCache.get(object, o ->  new ObjectLookupItemBuilder(o.ref(), SQLLanguage.INSTANCE));
        }
        if (language == PSQLLanguage.INSTANCE) {
            return psqlCache.get(object, o -> new ObjectLookupItemBuilder(o.ref(), PSQLLanguage.INSTANCE));
        }

        throw new IllegalArgumentException("Language " + language + " is not supported");
    }


    public void createLookupItem(Object source, CodeCompletionLookupConsumer consumer) {
        CodeCompletionContext context = consumer.getContext();

        CharSequence text = getText(context);
        if (text != null) {
            Icon icon = getIcon();

            String textHint = getTextHint();
            boolean bold = isBold();

            CodeCompletionLookupItem lookupItem;
            CodeCompletionSortingSettings sortingSettings = context.getCodeCompletionSettings().getSortingSettings();
            if (sortingSettings.isEnabled()) {
                int sortingIndex = sortingSettings.getSortingIndexFor(this);
                lookupItem = new CodeCompletionLookupItem(source, icon, text.toString(), textHint, bold, sortingIndex);
            } else {
                lookupItem = new CodeCompletionLookupItem(source, icon, text.toString(), textHint, bold);
            }
            adjustLookupItem(lookupItem);
            context.getResult().addElement(lookupItem);
        }
    }


    public abstract boolean isBold();

    public abstract CharSequence getText(CodeCompletionContext completionContext);

    protected void adjustLookupItem(CodeCompletionLookupItem lookupItem){}

    public abstract String getTextHint();

    public abstract Icon getIcon();
}
