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
import com.dbn.common.util.Strings;
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

import java.util.Collection;

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

        } else if (object instanceof Collection) {
            consumeCollection((Collection) object);

        } else {
            checkCancelled();
            LookupItemBuilder lookupItemBuilder = null;
            DBLanguage language = context.getLanguage();
            if (object instanceof DBObject) {
                DBObject dbObject = (DBObject) object;
                lookupItemBuilder = dbObject.getLookupItemBuilder(language);
            } else if (object instanceof DBObjectPsiElement) {
                DBObjectPsiElement objectPsiElement = (DBObjectPsiElement) object;
                lookupItemBuilder = objectPsiElement.ensureObject().getLookupItemBuilder(language);

            } else if (object instanceof TokenElementType) {
                TokenElementType tokenElementType = (TokenElementType) object;
                String text = tokenElementType.getText();
                if (Strings.isNotEmpty(text)) {
                    lookupItemBuilder = tokenElementType.getLookupItemBuilder(language);
                } else {
                    CodeCompletionFilterSettings filterSettings = context.getCodeCompletionFilterSettings();
                    TokenTypeCategory tokenTypeCategory = tokenElementType.getTokenTypeCategory();
                    if (tokenTypeCategory == TokenTypeCategory.OBJECT) {
                        TokenType tokenType = tokenElementType.tokenType;
                        DBObjectType objectType = tokenType.getObjectType();
                        if (objectType != null && filterSettings.acceptsRootObject(objectType)) {
                            lookupItemBuilder = new BasicLookupItemBuilder(
                                    tokenType.getValue(),
                                    objectType.getName(),
                                    objectType.getIcon());
                        }
                    } else if (filterSettings.acceptReservedWord(tokenTypeCategory)) {
                        lookupItemBuilder = tokenElementType.getLookupItemBuilder(language);
                    }
                }
            } else if (object instanceof IdentifierPsiElement) {
                IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) object;
                if (identifierPsiElement.isValid()) {
                    CharSequence chars = identifierPsiElement.getChars();
                    IdentifierType identifierType = identifierPsiElement.getIdentifierType();
                    if (identifierType == IdentifierType.VARIABLE) {
                        lookupItemBuilder = new VariableLookupItemBuilder(chars, true);
                    } else if (identifierType == IdentifierType.ALIAS) {
                        lookupItemBuilder = new AliasLookupItemBuilder(chars, true);
                    } else if (identifierType == IdentifierType.OBJECT && identifierPsiElement.isDefinition()) {
                        lookupItemBuilder = new IdentifierLookupItemBuilder(identifierPsiElement);

                    }
                }
            } else if (object instanceof String) {
                lookupItemBuilder = new AliasLookupItemBuilder((CharSequence) object, true);
            }

            if (lookupItemBuilder != null) {
                lookupItemBuilder.createLookupItem(object, this);
            }
        }
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
