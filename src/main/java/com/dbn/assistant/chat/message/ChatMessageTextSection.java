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
import com.intellij.openapi.util.TextRange;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.assistant.chat.message.ChatMessageLanguages.resolveLanguage;
import static com.dbn.assistant.chat.message.ChatMessageSectionType.CODE;
import static com.dbn.assistant.chat.message.ChatMessageSectionType.TEXT;

/**
 * Section of chat message, qualified with a language
 */
@Getter
@Setter
public class ChatMessageTextSection implements ChatMessageSection {

    private final String content;
    private final String languageId;

    // offsets in the original message
    private TextRange contentRange;
    private final ChatMessageSectionType type;

    public ChatMessageTextSection(String content, TextRange contentRange, @Nullable @NonNls String languageId) {
        this.content = content;
        this.contentRange = contentRange;
        this.languageId = languageId;
        this.type = languageId == null ? TEXT : CODE;
    }

    public int getContentStartOffset() {
        return contentRange.getStartOffset();
    }

    public int getContentEndOffset() {
        return contentRange.getEndOffset();
    }

    @Nullable
    public Language getLanguage() {
        return resolveLanguage(languageId);
    }

    public List<ChatMessageTextSection> asList() {
        List<ChatMessageTextSection> sections = new ArrayList<>();
        sections.add(this);
        return sections;
    }

    @Override
    public String toString() {
        return languageId == null ? content : "[" + languageId + "] " + content;
    }
}
