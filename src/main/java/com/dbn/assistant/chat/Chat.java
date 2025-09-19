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

package com.dbn.assistant.chat;

import com.dbn.assistant.AssistantType;
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.chat.message.ChatMessage;
import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Lists;
import com.dbn.common.util.TimeUtil;
import com.dbn.common.util.UUIDs;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.dbn.assistant.chat.message.AuthorType.AGENT;
import static com.dbn.assistant.chat.message.AuthorType.USER;
import static com.dbn.common.options.setting.Settings.longAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setLongAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Lists.isLast;
import static com.dbn.common.util.Strings.isNotEmpty;

@Getter
@Setter
public class Chat implements PersistentStateElement {
    private String id = UUIDs.compact();
    private String title;
    private ChatContext context;
    private List<ChatMessage> messages = new CopyOnWriteArrayList<>();
    private long timestamp = System.currentTimeMillis();

    private String sessionSignature;

    public Chat(ChatContext context) {
        this.context = context;
    }

    public boolean isInteractive() {
        return context.isInteractive();
    }

    public boolean isPersisted() {
        return isNotEmpty(title);
    }

    public boolean isSigned() {
        return isNotEmpty(sessionSignature);
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }

    public boolean isErrorsOnly() {
        return messages.stream().noneMatch(m -> m.getAuthor() == AGENT);
    }

    public void clear() {
        messages.clear();
    }

    public void addMessage(ChatMessage message) {
        if (messages.isEmpty()) {
            // reset the timestamp to reflect the real start of the chat
            timestamp = System.currentTimeMillis();
        }
        messages.add(message);
    }

    @Nullable
    public ChatMessage getLastMessage() {
        return Lists.lastElement(messages);
    }


    public void removeProgress() {
        messages.forEach(message -> {
            message.setProgress(false);
        });
    }

    public List<String> getUserPrompts() {
        return messages.
                stream().
                filter(m -> m.getAuthor() == USER).
                map(m -> m.getContent()).
                collect(Collectors.toList());
    }

    public boolean isOlderThan(long duration, TimeUnit unit) {
        return TimeUtil.isOlderThan(timestamp, duration, unit);
    }

    public boolean isRecentPrompt(ChatMessage message) {
        if (message.getAuthor() != USER) return false;
        if (messages.isEmpty()) return false;
        if (!isLast(messages, message)) return false;
        if (message.isOlderThan(10, TimeUnit.SECONDS)) return false;

        return true;

    }

    @Override
    public void readState(Element element) {
        id = stringAttribute(element, "id");
        title = stringAttribute(element, "title");
        sessionSignature = stringAttribute(element, "session-signature");
        timestamp = longAttribute(element, "timestamp", 0L);

        AssistantType assistantType = getContext().getAssistantType();
        List<Element> messagesElements = element.getChild("messages").getChildren();
        for(Element msgElement : messagesElements){
            ChatMessage chatMessage = new ChatMessage(assistantType);
            chatMessage.readState(msgElement);
            messages.add(chatMessage);
        }
        Element contextElement = element.getChild("context");
        context.readState(contextElement);
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "id", id);
        setStringAttribute(element, "title", title);
        setStringAttribute(element, "session-signature", sessionSignature);
        setLongAttribute(element, "timestamp", timestamp);
        Element messagesElement = newElement("messages");
        element.addContent(messagesElement);
        for (ChatMessage message : messages) {
            Element messageElement = newElement("message");
            messagesElement.addContent(messageElement);
            message.writeState(messageElement);
        }

        Element contextElement = newElement(element,"context");
        context.writeState(contextElement);
    }
}
