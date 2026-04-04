/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.assistant.chat.message.ui;

import com.dbn.assistant.chat.message.ChatMessageSection;
import com.dbn.assistant.chat.message.ChatMessageSectionType;
import com.dbn.assistant.chat.message.ChatMessageTextSection;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.intellij.lang.Language;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

@Getter
public abstract class ChatMessageSectionForm<T extends ChatMessageSection> extends DBNFormBase {
    private String content;
    private TextContent textContent;
    private final Function<String, TextContent > contentBuilder;
    private final T section;

    ChatMessageSectionForm(DBNForm parent, T section) {
        this(parent, section, c -> TextContent.plain(c));
    }

    ChatMessageSectionForm(DBNForm parent, T section, Function<String, TextContent> contentBuilder) {
        super(parent);
        this.section = section;
        this.contentBuilder = contentBuilder;
    }

    ChatMessageSectionType getSectionType() {
        return section == null ? ChatMessageSectionType.TEXT : section.getType();
    }

    public final void updateContent(ChatMessageTextSection section) {
        updateContent(section.getContent(), section.getLanguage());
    }

    protected final void updateContent(String content) {
        updateContent(content, null);
    }

    protected final void updateContent(String content, @Nullable Language language) {
        if (Objects.equals(this.content, content)) return;

        this.content = content;
        this.textContent = createTextContent(this.content);
        applyContent(this.textContent, language);
    }

    protected void rebuildContent() {
        textContent.rebuild();
        applyContent(this.textContent, null);
    }

    public void hideProcessingIndicator() {}

    protected final TextContent createTextContent(String content) {
        return contentBuilder.apply(content);
    }

    abstract protected void applyContent(TextContent content, @Nullable Language language);
}
