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
import com.dbn.code.common.completion.CodeCompletionContributor;
import com.dbn.code.common.completion.options.CodeCompletionSettings;
import com.dbn.code.common.completion.options.general.CodeCompletionFormatSettings;
import com.dbn.code.common.style.DBLCodeStyleManager;
import com.dbn.code.common.style.options.CodeStyleCaseOption;
import com.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dbn.common.util.Characters;
import com.dbn.common.util.Strings;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.QuotePair;
import com.dbn.object.DBSynonym;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBVirtualObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.util.Strings.toLowerCase;
import static com.dbn.common.util.Strings.toUpperCase;

public class ObjectLookupItemBuilder extends LookupItemBuilder {
    private final DBLanguage language;
    private final DBObjectRef objectRef;

    public ObjectLookupItemBuilder(DBObjectRef objectRef, DBLanguage language) {
        this.objectRef = objectRef;
        this.language = language;
    }

    @Override
    protected void adjustLookupItem(@NotNull CodeCompletionLookupItem lookupItem) {
        DBObject object = getObject();
        if (!isValid(object)) return;
        if (!object.needsNameQuoting()) return;

        DatabaseCompatibilityInterface compatibility = object.getCompatibilityInterface();
        String lookupString = object.getName();
        QuotePair quotes = compatibility.getDefaultIdentifierQuotes();
        lookupString = quotes.quote(lookupString);
        lookupItem.setLookupString(lookupString);


/*
            lookupItem.setInsertHandler(consumer.isAddParenthesis() ?
                                BracketsInsertHandler.INSTANCE :
                                BasicInsertHandler.INSTANCE);
*/

    }

    @Nullable
    public DBObject getObject() {
        return DBObjectRef.get(objectRef);
    }

    @Override
    public String getTextHint() {
        DBObject object = getObject();
        if (object != null) {
            DBObject parentObject = object.getParentObject();

            String typePrefix = "";
            if (object instanceof DBSynonym) {
                DBSynonym synonym = (DBSynonym) object;
                DBObjectType underlyingObjectType = synonym.getUnderlyingObjectType();
                if (underlyingObjectType != null) {
                    typePrefix = underlyingObjectType.getName() + ' ';
                }
            }

            return parentObject == null ?
                    typePrefix + object.getTypeName() :
                    typePrefix + object.getTypeName() + " (" +
                       parentObject.getTypeName() + ' ' +
                       parentObject.getName() + ')';
        }
        return "";
    }

    @Override
    public boolean isBold() {
        return false;
    }

    @Override
    public CharSequence getText(CodeCompletionContext context) {
        String text = objectRef.getObjectName();
        if (Strings.isNotEmptyOrSpaces(text)) {
            DBObject object = getObject();
            if (object instanceof DBVirtualObject && text.contains(CodeCompletionContributor.DUMMY_TOKEN)) {
                return null;
            }

            Project project = context.getFile().getProject();
            CodeStyleCaseSettings styleCaseSettings = DBLCodeStyleManager.getInstance(project).getCodeStyleCaseSettings(language);
            CodeStyleCaseOption caseOption = styleCaseSettings.getObjectCaseOption();

            text = caseOption.format(text);

            String userInput = context.getUserInput();
            CodeCompletionFormatSettings codeCompletionFormatSettings = CodeCompletionSettings.getInstance(project).getFormatSettings();
            if (Strings.isNotEmpty(userInput) && !text.startsWith(userInput) && !codeCompletionFormatSettings.isEnforceCodeStyleCase()) {
                char firstInputChar = userInput.charAt(0);
                char firstPresentationChar = text.charAt(0);

                if (Characters.equalIgnoreCase(firstInputChar, firstPresentationChar)) {
                    boolean upperCaseInput = Character.isUpperCase(firstInputChar);
                    boolean upperCasePresentation = Character.isUpperCase(firstPresentationChar);

                    if (Strings.isMixedCase(text)) {
                        if (upperCaseInput != upperCasePresentation) {
                            text = upperCaseInput ?
                                    toUpperCase(text) :
                                    toLowerCase(text);
                        }
                    } else {
                        text = upperCaseInput ?
                                toUpperCase(text) :
                                toLowerCase(text);
                    }
                } else {
                    return null;
                }
            }
            return text;
        }
        return null;
    }

    @Override
    public Icon getIcon() {
        DBObject object = getObject();
        return object == null ? objectRef.getObjectType().getIcon() : object.getIcon();
    }
}
