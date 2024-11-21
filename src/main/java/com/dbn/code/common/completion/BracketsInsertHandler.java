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

import com.dbn.code.common.lookup.CodeCompletionLookupItem;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;

public class BracketsInsertHandler extends BasicInsertHandler{
    public static final BracketsInsertHandler INSTANCE = new BracketsInsertHandler();

    @Override
    public void handleInsert(InsertionContext insertionContext, CodeCompletionLookupItem lookupElement) {
        Editor editor = insertionContext.getEditor();
        Document document = editor.getDocument();
        CaretModel caretModel = editor.getCaretModel();
        int startOffset = insertionContext.getStartOffset();
        char completionChar = insertionContext.getCompletionChar();

        int endOffset = startOffset + lookupElement.getLookupString().length();
        document.insertString(endOffset, "()");

        if (completionChar == ' ') {
            caretModel.moveCaretRelatively(3, 0, false, false, false);
        } else {
            caretModel.moveCaretRelatively(1, 0, false, false, false);
        }
    }
}