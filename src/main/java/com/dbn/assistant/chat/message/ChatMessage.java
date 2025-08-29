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

import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.chat.context.ChatContextImpl;
import com.dbn.assistant.editor.SQLChatMessageConverter;
import com.dbn.common.message.MessageType;
import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Strings;
import com.dbn.common.util.UUIDs;
import com.dbn.language.sql.SQLLanguage;
import com.intellij.lang.Language;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.readCdata;
import static com.dbn.common.options.setting.Settings.setBooleanAttribute;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.options.setting.Settings.writeCdata;
import static com.dbn.common.util.Lists.removeLast;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessage implements PersistentStateElement {
    /**
     * Unique identifier of the chat message to establish causality relations and chaining of messages
     */
    protected String id = UUIDs.regular();

    protected MessageType type = MessageType.NEUTRAL;
    protected AuthorType author;
    protected @NonNls String content;
    protected ChatContext context;
    protected boolean folded;

    private List<ChatMessageSection> sections;

    private transient boolean progress;


    /**
     * Creates a new ChatMessage
     *
     * @param type the message type (relevant for SYSTEM messages)
     * @param content the message content
     * @param author  the author of the message
     * @param context the context in which the chat message was produced
     */
    public ChatMessage(MessageType type, String content, AuthorType author, ChatContext context) {
        this.type = type;
        this.content = content.trim();
        this.author = author;
        this.context = context;
    }

    @NotNull
    public synchronized List<ChatMessageSection> getSections() {
        if (sections == null) {
            sections = buildSections();
        }
        return sections;
    }

    public void appendToken(String token){
        content = content + token;
        if (sections == null) {
            sections = buildSections();
        } else {
            ChatMessageSection lastSection = removeLast(sections);

            int shift = lastSection == null ? 0 : lastSection.getContentStartOffset();
            List<ChatMessageSection> deltaSections = buildSections(shift);

            sections.addAll(deltaSections);
        }
    }

    private List<ChatMessageSection> buildSections() {
        return buildSections(0);
    }

    /**
     * Breaks message contents into sections, to allow different styling of the content within same response.
     * Background: responses from the AI backends may contain a sequence of text and code sections.
     * Code is typically demarcated by ``` (3 single quotes) followed by code content and closed with again with 3 single quotes
     *
     * @return a list of {@link ChatMessageSection} with the different sections
     */
    private List<ChatMessageSection> buildSections(int shift) {
        if (isSqlCodeContent()) {
            // output is expected to be SQL code based on the author, action and content
            return new ChatMessageSection(content, basicContentRange(content, shift), "sql").asList();
        }

        if (author.isOneOf(AuthorType.USER, AuthorType.SYSTEM)) {
            // output is already expected to be plain text
            return new ChatMessageSection(content, basicContentRange(content, shift), null).asList();
        }

        return ChatMessageParser.parse(content, shift);
    }

    private @NotNull TextRange basicContentRange(String content, int shift) {
        return new TextRange(shift, shift + content.length());
    }

    private boolean hasCodeSections() {
        return content.contains("```");
    }

    private boolean isSelectStatement() {
        // TODO move to AssistantAdapter (quick workaround for Select AI context)
        if (Strings.isEmpty(context.getProfileName())) return false;

        return
            StringUtil.startsWithIgnoreCase(content, "select") ||
            StringUtil.startsWithIgnoreCase(content, "with");
    }

    private boolean isSqlCodeContent() {
        // special case of SHOW_SQL agent responses in plain text which are actually sql blocks

        if (author != AuthorType.AGENT) return false;
        if (hasCodeSections()) return false;
        if (!isSelectStatement()) return false;

        return true;
    }

    public String outputForLanguage(Language language) {
        if (language == SQLLanguage.INSTANCE) {
            return SQLChatMessageConverter.INSTANCE.convert(this);
            //.. TODO more languages if functionality is integrated in non-SQL editors
        }
        return content;
    }


    @Override
    public void readState(Element element) {
        id = stringAttribute(element, "id");
        type = enumAttribute(element, "type", type);
        author = enumAttribute(element, "author", AuthorType.class);
        folded = booleanAttribute(element, "folded", folded);

        Element contentElement = element.getChild("content");
        content = readCdata(contentElement);

        Element contextElement = element.getChild("context");
        context = new ChatContextImpl();
        context.readState(contextElement);
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "id", id);
        setEnumAttribute(element, "type", type);
        setEnumAttribute(element, "author", author);
        setBooleanAttribute(element, "folded", folded);

        Element contentElement = newElement(element,"content");
        writeCdata(contentElement, content);

        Element contextElement = newElement(element,"context");
        context.writeState(contextElement);
    }
}
