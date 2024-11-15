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

import java.util.List;

@Getter
public abstract class GenericChatMessageConverter implements ChatMessageConverter{
    private final Language language;

    protected GenericChatMessageConverter(Language language) {
        this.language = language;
    }

    @Override
    public final String convert(ChatMessage chatMessage) {
        StringBuilder builder = new StringBuilder();
        List<ChatMessageSection> sections = chatMessage.getSections();
        for (ChatMessageSection section : sections) {
            String content = section.getContent();
            if (section.getLanguage() == language) {
                // no alteration to the content if language matches the target language
                builder.append(content);
            } else {
                // attempt to comment the content if not matching the target language
                builder.append(getBockCommentStart());
                builder.append(adjustComment(content));
                builder.append(getBockCommentEnd());
            }
        }
        return builder.toString().trim();
    }

    protected abstract String getBockCommentStart();

    protected abstract String getBockCommentEnd();

    /**
     * Adjust the content of the given comment (e.g. alignment, highlighting aso...)
     * @param comment the content of the comment to adjust
     * @return the adjusted comment
     */
    protected abstract String adjustComment(String comment);
}
