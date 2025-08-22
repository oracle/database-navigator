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

package com.dbn.assistant.chat.message;

import com.intellij.lang.Language;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.dbn.assistant.chat.message.ChatMessageLanguages.resolveLanguage;

/**
 * Section of chat message, qualified with a language
 */
@Getter
@Setter
public class ChatMessageSection {

    private String content;
    private final String languageId;

    // offsets in the original message
    private final int startOffset;
    private int endOffset;

    public ChatMessageSection(String content, @Nullable @NonNls String languageId) {
        this(0, content, languageId);
    }

    public ChatMessageSection(int startOffset, String content, @Nullable @NonNls String languageId) {
        this.startOffset = startOffset;
        this.content = content.trim();
        this.languageId = languageId;

        updateEndOffset();
    }

    @Nullable
    public Language getLanguage() {
        return resolveLanguage(languageId);
    }

    public void append(String content) {
        this.content = this.content + "\n" + content;
        updateEndOffset();
    }

    public void setContent(String content) {
        this.content = content;
        updateEndOffset();
    }

    private void updateEndOffset() {
        this.endOffset = this.startOffset + this.content.length();
    }

    public List<ChatMessageSection> asList() {
        return List.of(this);
    }

    @Override
    public String toString() {
        return languageId == null ? content : "[" + languageId + "] " + content;
    }
}
