/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.assistant.state;

import com.dbn.assistant.AssistantMode;
import com.dbn.assistant.AssistantType;
import com.dbn.assistant.adapter.AssistantAdapter;
import com.dbn.assistant.adapter.AssistantAdapters;
import com.dbn.assistant.chat.Chat;
import com.dbn.assistant.chat.ChatAvailability;
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.chat.context.ChatContextImpl;
import com.dbn.assistant.tool.approval.AssistantToolApprovals;
import com.dbn.assistant.tool.config.AssistantToolSettings;
import com.dbn.common.feature.FeatureAcknowledgement;
import com.dbn.common.feature.FeatureAvailability;
import com.dbn.common.property.PropertyHolderBase;
import com.dbn.common.state.PersistentStateElement;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.object.DBTable;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.util.UserDataHolderBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dbn.assistant.chat.ChatAvailability.BUSY_INITIALIZING;
import static com.dbn.assistant.chat.ChatAvailability.BUSY_QUERYING;
import static com.dbn.assistant.chat.ChatAvailability.INACTIVE_CHAT_SELECTED;
import static com.dbn.assistant.chat.ChatAvailability.NOT_INITIALIZED;
import static com.dbn.assistant.chat.ChatAvailability.NOT_SUPPORTED;
import static com.dbn.assistant.state.AssistantStatus.INITIALIZING;
import static com.dbn.assistant.state.AssistantStatus.QUERYING;
import static com.dbn.assistant.state.AssistantStatus.UNAVAILABLE;
import static com.dbn.assistant.state.AssistantStatus.VALUES;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.connectionIdAttribute;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Strings.isNotEmpty;

/**
 * Assistant state holder
 * This class represents the state of the DB Assistant for a given connection, as well as the chat-box state.
 * It encapsulates the current profiles, selected profile,
 * a history of questions, the AI answers, and the current connection.
 *
 * @author Ayoub Aarrasse (Oracle)
 * @author Emmanuel Jannetti (Oracle)
 */
@Setter
@Getter
@NoArgsConstructor
public class AssistantState extends PropertyHolderBase.IntStore<AssistantStatus> implements PersistentStateElement, UserDataHolder {

    private FeatureAvailability availability = FeatureAvailability.UNCERTAIN;
    private FeatureAcknowledgement acknowledgement = FeatureAcknowledgement.NONE;

    private ConnectionId connectionId;
    private AssistantType assistantType;
    private Map<String, Chat> chats = new LinkedHashMap<>();

    private String currentChatId;
    private String currentSessionSignature; // the resourceId of the com.dbn.connection.jdbc.Resource
    private String defaultProfileName;
    private ChatContext lastContext;


    @Delegate
    private final UserDataHolder userData = new UserDataHolderBase();

    public AssistantState(ConnectionId connectionId, AssistantType assistantType) {
        this.connectionId = connectionId;
        this.assistantType = assistantType;
    }

    public synchronized ChatContext getLastContext() {
        if (lastContext == null) {
            lastContext = new ChatContextImpl(assistantType);
        }
        return lastContext;
    }

    public AssistantToolSettings getToolSettings() {
        return AssistantToolSettings.get(this);
    }

    public AssistantToolApprovals getToolApprovals() {
        AssistantToolSettings settings = getToolSettings();
        return settings.getApprovals();
    }

    @Override
    protected AssistantStatus[] properties() {
        return VALUES;
    }

    public AssistantAdapter getAssistantAdapter() {
        return AssistantAdapters.get(assistantType);
    }

    public ConnectionHandler getConnection() {
        return ConnectionHandler.ensure(connectionId);
    }

    public Chat getChat(String chatId) {
        return chats.get(chatId);
    }

    @Nullable
    public Chat getChatForSource(String sourceId) {
        for (Chat chat : chats.values()) {
            if (Objects.equals(chat.getSourceId(), sourceId)) return chat;
        }
        return null;
    }

    public Set<String> getChatNames() {
        return chats.
                values().
                stream().
                map(c -> c.getTitle()).
                filter(t -> isNotEmpty(t)).
                collect(Collectors.toSet());
    }

    public List<Chat> getSavedChats() {
        return chats
                .values()
                .stream()
                .sorted(Comparator.comparing(c -> ((Chat) c).getTimestamp()).reversed())
                .filter(c -> c.isPersisted())
                .toList();
    }

    public Chat createChat(ChatContext chatContext) {
        Chat conversation = new Chat(chatContext);
        conversation.setSessionSignature(currentSessionSignature);
        String conversationId = conversation.getId();

        chats.put(conversationId, conversation);
        setCurrentChatId(conversationId);
        return conversation;
    }

    public void deleteChat(String conversationId) {
        chats.remove(conversationId);
    }

    public void deleteChats(List<String> conversationIds) {
        conversationIds.forEach(id -> deleteChat(id));
    }

    public void deleteObsoleteChats() {
        chats.keySet().removeIf(id -> isObsoleteChat(id));
    }

    public boolean isCurrentConversation(Chat conversation) {
        return Objects.equals(currentChatId, conversation.getId());
    }

    private boolean isObsoleteChat(String conversationId) {
        Chat conversation = getChat(conversationId);
        if (conversation.isPersisted()) return false;
        if (isCurrentConversation(conversation)) return false;
        if (conversation.isEmpty()) return true; // empty unsaved conversations are
        return false;
    }

    public synchronized Chat getCurrentChat() {
        Chat currentChat = chats.get(currentChatId);
        if (currentChat == null) {
            currentChat = new Chat(getLastContext());
            currentChat.setSessionSignature(currentSessionSignature);
            currentChatId = currentChat.getId();
            chats.put(currentChatId, currentChat);
        }
        return currentChat;
    }

    public ChatContext getCurrentContext() {
        return getCurrentChat().getContext();
    }

    public void setCurrentChatId(String id) {
        currentChatId = id;

        Chat currentChat = getCurrentChat();
        lastContext = currentChat.getContext();
    }

    public void setCurrentContext(ChatContext chatContext) {
        Chat currentChat = getCurrentChat();
        currentChat.setContext(chatContext);
        lastContext = currentChat.getContext();
    }


    public AssistantMode getAssistantMode() {
        return getCurrentContext().getAssistantMode();
    }

    public DBObjectRef<DBTable> getEmbeddingTable() {
        return getCurrentContext().getEmbeddingTable();
    }

    public void setCurrentSessionSignature(String currentSessionSignature) {
        this.currentSessionSignature = currentSessionSignature;
        Chat conversation = getCurrentChat();

        if (!conversation.isSigned() || !conversation.isInteractive() || conversation.isEmpty() || conversation.isErrorsOnly()) {
            // safe to update conversation signature
            conversation.setSessionSignature(currentSessionSignature);
        }
    }

    public boolean isSupported() {
        return availability == FeatureAvailability.AVAILABLE;
    }

    /**
     * State utility indicating the feature is initialized and ready to use
     * @return true if the chat box is properly initialized and can be interacted with
     */
    public boolean isAvailable() {
        return isSupported() &&
                isNot(INITIALIZING) &&
                isNot(UNAVAILABLE) &&
                isNot(QUERYING);
    }

    public ChatAvailability getChatAvailability() {
        if (!isSupported()) return NOT_SUPPORTED;

        if (is(UNAVAILABLE)) return NOT_INITIALIZED;
        if (is(INITIALIZING)) return BUSY_INITIALIZING;
        if (is(QUERYING)) return BUSY_QUERYING;
        if (!isCurrentChatActive()) return INACTIVE_CHAT_SELECTED;

        AssistantAdapter assistantAdapter = getAssistantAdapter();
        return assistantAdapter.getChatAvailability(connectionId);
    }

    public boolean isCurrentChatInteractive() {
        Chat conversation = getCurrentChat();
        return conversation.isInteractive();
    }

    public boolean isCurrentChatActive() {
        AssistantAdapter assistantAdapter = getAssistantAdapter();
        return assistantAdapter.isCurrentChatActive(connectionId);
    }

    public boolean isCurrentContextEnabled() {
        AssistantAdapter assistantAdapter = getAssistantAdapter();
        return assistantAdapter.isCurrentContextEnabled(connectionId);
    }

    public boolean isCurrentContextValid() {
        AssistantAdapter assistantAdapter = getAssistantAdapter();
        return assistantAdapter.isCurrentContextValid(connectionId);
    }

    @Override
    public void readState(Element element) {
        connectionId = connectionIdAttribute(element, "connection-id");
        assistantType = enumAttribute(element, "assistant-type", AssistantType.PUBLIC);
        defaultProfileName = stringAttribute(element, "default-profile-name");
        currentChatId = stringAttribute(element, "selected-chat-id");
        availability = enumAttribute(element, "availability", availability);
        acknowledgement = enumAttribute(element, "acknowledgement", acknowledgement);

        Element chatsElement = element.getChild("chats");
        List<Element> chatElements = childrenOf(chatsElement);

        for (Element chatElement : chatElements) {
            ChatContext chatContext = new ChatContextImpl(assistantType);
            Chat chat = new Chat(chatContext);
            chat.readState(chatElement);
            chats.put(chat.getId(), chat);
        }

        AssistantToolSettings toolSettings = getToolSettings();
        Element toolsElement = element.getChild("tools");
        toolSettings.readState(toolsElement);
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "connection-id", connectionId.id());
        setEnumAttribute(element, "assistant-type", assistantType);
        setStringAttribute(element, "default-profile-name", defaultProfileName);
        setStringAttribute(element, "selected-chat-id", currentChatId);
        setEnumAttribute(element, "availability", availability);
        setEnumAttribute(element, "acknowledgement", acknowledgement);

        if (!chats.isEmpty()) {
            Element chatsElement = newElement(element, "chats");
            for (Chat chat : chats.values()) {
                if (isObsoleteChat(chat.getId())) continue;

                Element chatElement = newElement(chatsElement, "chat");
                chat.writeState(chatElement);
            }
        }

        Element toolsElement = newElement(element, "tools");
        AssistantToolSettings toolSettings = getToolSettings();
        toolSettings.writeState(toolsElement);

    }

    public Project getProject() {
        return getConnection().getProject();
    }
}
