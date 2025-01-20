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
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.dbn.assistant.chat.message.ChatMessageLanguages.resolveLanguage;

/**
 * Section of chat message, qualified with a language
 */
@Getter
public class ChatMessageSection {

    private String content;
    private final String languageId;

    public ChatMessageSection(String content, @Nullable @NonNls String languageId) {
        this.content = content.trim();
        this.languageId = languageId;
    }

    @Nullable
    public Language getLanguage() {
        return resolveLanguage(languageId);
    }

    public void append(String content) {
        this.content = this.content + "\n" + content;
    }

    public List<ChatMessageSection> asList() {
        return List.of(this);
    }
}
