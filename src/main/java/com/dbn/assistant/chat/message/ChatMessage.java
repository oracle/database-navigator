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

import com.dbn.assistant.AssistantType;
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.chat.context.ChatContextImpl;
import com.dbn.assistant.editor.SQLChatMessageConverter;
import com.dbn.assistant.tool.execution.AssistantToolInvocation;
import com.dbn.assistant.tool.execution.AssistantToolResponse;
import com.dbn.common.message.MessageType;
import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Strings;
import com.dbn.common.util.TimeUtil;
import com.dbn.common.util.UUIDs;
import com.dbn.language.sql.SQLLanguage;
import com.intellij.lang.Language;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.longAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.readCdata;
import static com.dbn.common.options.setting.Settings.setBooleanAttribute;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setLongAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.options.setting.Settings.writeCdata;
import static com.dbn.common.util.Lists.first;
import static com.dbn.common.util.Lists.last;
import static com.dbn.common.util.Lists.lastElement;
import static com.dbn.common.util.Lists.removeLast;

@Getter
@Setter
public class ChatMessage implements PersistentStateElement {
    /**
     * Unique identifier of the chat message to establish causality relations and chaining of messages
     */
    protected String id = UUIDs.regular();

    private final AssistantType assistantType;
    protected MessageType type = MessageType.NEUTRAL;
    protected AuthorType author;
    protected @NonNls String content;
    protected ChatContext context;
    protected boolean folded;
    private long timestamp = System.currentTimeMillis();

    private List<ChatMessageSection> sections;
    private List<ChatMessageToolSection> toolSections = new ArrayList<>();

    private transient boolean progress;

    public ChatMessage(AssistantType assistantType) {
        this.assistantType = assistantType;
    }

    /**
     * Creates a new ChatMessage
     *
     * @param type the message type (relevant for SYSTEM messages)
     * @param content the message content
     * @param author  the author of the message
     * @param context the context in which the chat message was produced
     */
    public ChatMessage(AssistantType assistantType, MessageType type, String content, AuthorType author, ChatContext context) {
        this.assistantType = assistantType;
        this.type = type;
        this.content = removeCodeBlockIndents(content.trim());
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

    public void appendToken(String token) {
        int lastToolOffset = getLastToolOffset();
        int currentOffset = content.length();

        content = content + token;
        if (token.contains("```")) {
            content = removeCodeBlockIndents(content);
        }

        if (sections == null) {
            sections = buildSections();
            return;
        }

        if (lastToolOffset == currentOffset) {
            int startOffset = getLastSectionEndOffset();
            List<ChatMessageSection> deltaSections = buildSections(startOffset);

            sections.addAll(deltaSections);
        } else {
            removeLast(sections);

            int startOffset = getLastSectionEndOffset();
            List<ChatMessageSection> deltaSections = buildSections(startOffset);

            sections.addAll(deltaSections);
        }
    }

    private static String removeCodeBlockIndents(String content) {
        // remove code-block indents (md parser does not properly demarcate them if indented)
        return content.replaceAll("(?m)^[ \\t]+(?=```)", "");
    }

    private int getLastSectionEndOffset() {
        ChatMessageSection lastSection = lastElement(sections);
        return lastSection == null ? 0 : lastSection.getContentEndOffset();
    }

    private int getLastToolOffset() {
        ChatMessageToolSection lastToolSection = lastElement(toolSections);
        return lastToolSection == null ? 0 : lastToolSection.getOffset();
    }

    private int[] getSliceOffsets() {
        Set<Integer> offsets = new LinkedHashSet<>();
        for (ChatMessageToolSection toolSection : toolSections) {
            offsets.add(toolSection.getOffset());
        }
        return offsets.stream().mapToInt(i -> i).toArray();
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
    private List<ChatMessageSection> buildSections(int offset) {
        int contentLength = content.length();
        if (isSqlCodeContent()) {
            // output is expected to be SQL code based on the author, action and content
            TextRange textRange = new TextRange(offset, contentLength);
            return new ChatMessageSection(content, textRange, "sql").asList();
        }

        if (author.isOneOf(AuthorType.USER, AuthorType.SYSTEM)) {
            // output is already expected to be plain text
            TextRange textRange = new TextRange(offset, contentLength);
            return new ChatMessageSection(content, textRange, null).asList();
        }

        return ChatMessageParser.parse(content, offset, getSliceOffsets());
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

    public boolean isOlderThan(long duration, TimeUnit unit) {
        return TimeUtil.isOlderThan(timestamp, duration, unit);
    }

    public void appendToolRequest(AssistantToolInvocation toolInvocation) {
        ChatMessageToolSection toolSection = new ChatMessageToolSection(content.length(), toolInvocation);
        toolSections.add(toolSection);
    }

    public void appendToolResponse(String requestId, String toolName, String toolResponse) {
        ChatMessageToolSection toolSection = findToolSection(requestId, toolName);
        if (toolSection == null) return;

        AssistantToolResponse response = new AssistantToolResponse(toolResponse);
        toolSection.getInvocation().setResponse(response);
    }

    private ChatMessageToolSection findToolSection(String requestId, String toolName) {
        return requestId == null ? // unsigned requests
                last(toolSections, s -> Objects.equals(s.getToolName(), toolName)) :
                first(toolSections, s -> Objects.equals(s.getToolRequestId(), requestId));
    }

    @Override
    public void readState(Element element) {
        id = stringAttribute(element, "id");
        type = enumAttribute(element, "type", type);
        author = enumAttribute(element, "author", AuthorType.class);
        timestamp = longAttribute(element, "timestamp", timestamp);
        folded = booleanAttribute(element, "folded", folded);

        Element contentElement = element.getChild("content");
        content = readCdata(contentElement);

        Element contextElement = element.getChild("context");
        context = new ChatContextImpl(assistantType);
        context.readState(contextElement);

        Element toolsElement = element.getChild("tools");
        List<Element> toolElements = childrenOf(toolsElement);
        for (Element toolElement : toolElements) {
            ChatMessageToolSection toolSection = new ChatMessageToolSection();
            toolSection.readState(toolElement);
            toolSections.add(toolSection);
        }

    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "id", id);
        setEnumAttribute(element, "type", type);
        setEnumAttribute(element, "author", author);
        setLongAttribute(element, "timestamp", timestamp);
        setBooleanAttribute(element, "folded", folded);

        Element contentElement = newElement(element,"content");
        writeCdata(contentElement, content);

        Element contextElement = newElement(element,"context");
        context.writeState(contextElement);

        if (!toolSections.isEmpty()) {
            Element toolsElement = newElement(element,"tools");
            for (ChatMessageToolSection toolSection : toolSections) {
                Element toolElement = newElement(toolsElement, "tool");
                toolSection.writeState(toolElement);
            }
        }
    }
}
