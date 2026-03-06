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

package com.dbn.code.common.completion;

import com.dbn.code.common.completion.options.filter.CodeCompletionFilterSettings;
import com.dbn.code.common.lookup.AliasLookupItemBuilder;
import com.dbn.code.common.lookup.BasicLookupItemBuilder;
import com.dbn.code.common.lookup.IdentifierLookupItemBuilder;
import com.dbn.code.common.lookup.LookupItemBuilder;
import com.dbn.code.common.lookup.VariableLookupItemBuilder;
import com.dbn.common.consumer.CancellableConsumer;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.TokenTypeCategory;
import com.dbn.language.common.element.impl.TokenElementType;
import com.dbn.language.common.element.util.IdentifierType;
import com.dbn.language.common.psi.IdentifierPsiElement;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectPsiElement;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import static com.dbn.language.common.element.util.IdentifierType.ALIAS;
import static com.dbn.language.common.element.util.IdentifierType.OBJECT;
import static com.dbn.language.common.element.util.IdentifierType.VARIABLE;

@Getter
public class CodeCompletionLookupConsumer implements CancellableConsumer<Object> {
    private final CodeCompletionContext context;

    CodeCompletionLookupConsumer(CodeCompletionContext context) {
        this.context = context;
    }

    @Override
    public void accept(Object object) {
        if (object instanceof Object[]) {
            consumeArray((Object[]) object);

        } else if (object instanceof Collection collection) {
            consumeCollection(collection);

        } else {
            LookupItemBuilder builder = resolveBuilder(object);
            if (builder == null) return;

            builder.createLookupItem(object, this);
        }
    }

    private @Nullable LookupItemBuilder resolveBuilder(Object object) {
        checkCancelled();
        DBLanguage language = context.getLanguage();
        if (object instanceof DBObject dbObject) {
            return dbObject.getLookupItemBuilder(language);
        }

        if (object instanceof DBObjectPsiElement objectPsiElement) {
            return objectPsiElement.ensureObject().getLookupItemBuilder(language);
        }

        if (object instanceof TokenElementType tokenElementType) {
            return acceptToken(tokenElementType);
        }

        if (object instanceof IdentifierPsiElement identifierPsiElement) {
            return acceptIdentifier(identifierPsiElement);
        }

        if (object instanceof String) {
            return new AliasLookupItemBuilder((CharSequence) object, true);
        }
        return null;
    }

    private LookupItemBuilder acceptToken(TokenElementType tokenElementType) {
        DBLanguage language = context.getLanguage();
        if (tokenElementType.text != null) {
            return tokenElementType.getLookupItemBuilder(language);
        }

        CodeCompletionFilterSettings filterSettings = context.getCodeCompletionFilterSettings();
        TokenTypeCategory tokenTypeCategory = tokenElementType.getTokenTypeCategory();
        if (tokenTypeCategory == TokenTypeCategory.OBJECT) {
            TokenType tokenType = tokenElementType.tokenType;
            DBObjectType objectType = tokenType.getObjectType();
            if (objectType != null && filterSettings.acceptsRootObject(objectType)) {
                return new BasicLookupItemBuilder(
                        tokenType.getValue(),
                        objectType.getName(),
                        objectType.getIcon());
            }
        } else if (filterSettings.acceptReservedWord(tokenTypeCategory)) {
            return tokenElementType.getLookupItemBuilder(language);
        }
        return null;
    }

    private LookupItemBuilder acceptIdentifier(IdentifierPsiElement identifierPsiElement) {
        if (!identifierPsiElement.isValid()) return null;
        if (identifierPsiElement.getChars().equals(context.getUserInput())) return null;

        CharSequence chars = identifierPsiElement.getChars();
        IdentifierType identifierType = identifierPsiElement.getIdentifierType();
        if (identifierType == VARIABLE) {
            return new VariableLookupItemBuilder(chars, true);
        }

        if (identifierType == ALIAS) {
            return new AliasLookupItemBuilder(chars, true);
        }

        if (identifierType == OBJECT && identifierPsiElement.isDefinition()) {
            return new IdentifierLookupItemBuilder(identifierPsiElement);

        }
        return null;
    }

    private void consumeArray(Object[] array) {
        checkCancelled();
        if (array == null) return;

        for (Object element : array) {
            checkCancelled();
            accept(element);
        }
    }

    private void consumeCollection(Collection<Object> objects) {
        checkCancelled();
        if (objects == null) return;
        if (objects.isEmpty()) return;

        for (Object element : objects) {
            checkCancelled();
            accept(element);
        }
    }

    public void checkCancelled() {
        if (context.getResult().isStopped() || context.getQueue().isFinished()) {
            context.cancel();
            throw new CodeCompletionCancelledException();
        }
    }
}
