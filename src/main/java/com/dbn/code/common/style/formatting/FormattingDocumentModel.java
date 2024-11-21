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

package com.dbn.code.common.style.formatting;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FormattingDocumentModel implements com.intellij.formatting.FormattingDocumentModel {
    @Override
    public int getLineNumber(int offset) {
        return 0;
    }

    @Override
    public int getLineStartOffset(int line) {
        return 0;
    }

    @Override
    public CharSequence getText(final TextRange textRange) {
        return null;
    }

    @Override
    public int getTextLength() {
        return 0;
    }

    @Override
    @NotNull
    public Document getDocument() {
        return null;
    }

    @Override
    public boolean containsWhiteSpaceSymbolsOnly(int startOffset, int endOffset) {
        return false;
    }

    @Override
    @NotNull
    public CharSequence adjustWhiteSpaceIfNecessary(@NotNull CharSequence whiteSpaceText, int startOffset, int endOffset, @Nullable ASTNode nodeAfter, boolean changedViaPsi) {
        return whiteSpaceText;
    }

    @NotNull
    public CharSequence adjustWhiteSpaceIfNecessary(@NotNull CharSequence charSequence, int i, int i1, boolean b) {
        return charSequence;
    }

    @NotNull
    public CharSequence adjustWhiteSpaceIfNecessary(@NotNull CharSequence whiteSpaceText, int startOffset, int endOffset) {
        return whiteSpaceText;
    }
}
