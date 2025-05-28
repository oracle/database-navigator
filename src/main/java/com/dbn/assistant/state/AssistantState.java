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

package com.dbn.assistant.state;

import com.dbn.assistant.DatabaseAssistantType;
import com.dbn.assistant.chat.ChatContext;
import com.dbn.assistant.chat.ChatConversation;
import com.dbn.assistant.chat.PersistentChatConversation;
import com.dbn.assistant.chat.message.PersistentChatMessage;
import com.dbn.assistant.chat.window.PromptAction;
import com.dbn.assistant.provider.AIModel;
import com.dbn.common.feature.FeatureAcknowledgement;
import com.dbn.common.feature.FeatureAvailability;
import com.dbn.common.property.PropertyHolderBase;
import com.dbn.common.state.PersistentStateElement;
import com.dbn.connection.ConnectionId;
import com.dbn.object.DBAIProfile;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
 *
 * @author Ayoub Aarrasse (Oracle)
 * @author Emmanuel Jannetti (Oracle)
 */
@Setter
@Getter
@NoArgsConstructor
public class AssistantState extends PropertyHolderBase.IntStore<AssistantStatus> implements PersistentStateElement {

  private FeatureAvailability availability = FeatureAvailability.UNCERTAIN;
  private FeatureAcknowledgement acknowledgement = FeatureAcknowledgement.NONE;

  private ConnectionId connectionId;
  private DatabaseAssistantType assistantType = DatabaseAssistantType.GENERIC;
  private Map<String, ChatConversation> conversations = new LinkedHashMap<>();
  private String defaultProfileName;
  private String currentConversationId;

  public static final short MAX_CHAR_MESSAGE_COUNT = 100;

  public AssistantState(ConnectionId connectionId) {
    this.connectionId = connectionId;
  }

  @Override
  protected AssistantStatus[] properties() {
    return AssistantStatus.VALUES;
  }

  public String getAssistantName() {
    switch (assistantType) {
      case SELECT_AI: return txt("app.assistant.title.DatabaseAssistantName_SELECT_AI");
      case GENERIC:
      default: return txt("app.assistant.title.DatabaseAssistantName_GENERIC");
    }
  }

  public ChatConversation getConversation(String conversationId) {
    return conversations.get(conversationId);
  }

  public Set<String> getConversationTitles() {
    return conversations.
            values().
            stream().
            map(c -> c.getTitle()).
            filter(t -> isNotEmpty(t)).
            collect(Collectors.toSet());
  }

  public List<ChatConversation> getSavedConversations() {
    return conversations.
            values().
            stream().
            filter(c -> c.isPersisted()).
            collect(Collectors.toList());
  }

  public ChatConversation createConversation(ChatContext chatContext) {
    ChatConversation conversation = new PersistentChatConversation();
    conversation.setContext(chatContext);
    String conversationId = conversation.getId();

    conversations.put(conversationId, conversation);
    setCurrentConversationId(conversationId);
    return conversation;
  }

  public void deleteConversation(String conversationId){
    conversations.remove(conversationId);
  }

  public void deleteConversations(List<String> conversationIds){
    conversationIds.forEach(id -> deleteConversation(id));
  }

  public void deleteObsoleteConversations() {
    conversations.keySet().removeIf(id -> isObsoleteConversation(id));
  }

  public boolean isCurrentConversation(ChatConversation conversation) {
    return Objects.equals(currentConversationId, conversation.getId());
  }

  private boolean isObsoleteConversation(String conversationId) {
    ChatConversation conversation = getConversation(conversationId);
    if (conversation.isPersisted()) return false;
    if (isCurrentConversation(conversation)) return false;
    if (conversation.isEmpty()) return true; // empty unsaved conversations are
    return false;
  }

  public synchronized ChatConversation getCurrentConversation() {
    ChatConversation currentConversation = conversations.get(currentConversationId);
    if (currentConversation == null) {
      currentConversation = new PersistentChatConversation();
      currentConversation.setContext(new ChatContext());
      currentConversationId = currentConversation.getId();
      conversations.put(currentConversationId, currentConversation);
    }
    return currentConversation;
  }

  public ChatContext getCurrentContext() {
    return getCurrentConversation().getContext();
  }

  public void setCurrentContext(ChatContext context) {
    getCurrentConversation().setContext(context);
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
            isNot(AssistantStatus.INITIALIZING) &&
            isNot(AssistantStatus.UNAVAILABLE) &&
            isNot(AssistantStatus.QUERYING);
  }

  public boolean isPromptingAvailable() {
    if (!isAvailable()) return false;

    ChatContext context = getCurrentContext();
    if (!context.isInitialized()) return false;

    ChatConversation conversation = getCurrentConversation();
    if (!conversation.isActive()) return false;

    return true;
  }

  public void setDefaultProfile(@Nullable DBAIProfile profile) {
    setDefaultProfileName(profile == null ? null : profile.getName());
  }

  @Nullable
  public String getSelectedProfileName() {
    ChatContext context = getCurrentContext();
    return context.getProfile();
  }

  @Nullable
  public String getSelectedModelName() {
    ChatContext context = getCurrentContext();
    AIModel model = context.getModel();
    return model == null ? null : model.getId();
  }

  public PromptAction getSelectedAction() {
    return getCurrentConversation().getContext().getAction();
  }

  public void addMessages(List<PersistentChatMessage> messages) {
   getCurrentConversation().addMessages(messages);
  }

  public void clearMessages() {
    getCurrentConversation().getMessages().clear();
  }

  @Override
  public void readState(Element element) {
    connectionId = connectionIdAttribute(element, "connection-id");
    defaultProfileName = stringAttribute(element, "default-profile-name");
    currentConversationId = stringAttribute(element, "selected-conversation-id");
    assistantType = enumAttribute(element, "assistant-type", assistantType);
    availability = enumAttribute(element, "availability", availability);
    acknowledgement = enumAttribute(element, "acknowledgement", acknowledgement);

    Element conversationsElement = element.getChild("conversations");
    List<Element> conversationElements = childrenOf(conversationsElement);
    for (Element conversationElement : conversationElements) {
      PersistentChatConversation conversation = new PersistentChatConversation();
      conversation.readState(conversationElement);
      conversations.put(conversation.getId(), conversation);
    }

    //TODO to be removed later
    Element messagesElement = element.getChild("messages");
    List<Element> messageElements = childrenOf(messagesElement);
    String selectedProfileName = stringAttribute(element, "selected-profile-name");
    String selectedModelName = stringAttribute(element, "selected-model-name");
    PromptAction selectedAction = enumAttribute(element, "selected-action", PromptAction.class);
    if(selectedProfileName != null) {
      ChatContext context = new ChatContext(selectedProfileName, AIModel.forId(selectedModelName), selectedAction, false);
      ChatConversation currentConversation = createConversation(context);
      for (Element messageElement : messageElements) {
        PersistentChatMessage message = new PersistentChatMessage();
        message.readState(messageElement);
        currentConversation.addMessage(message);
      }
    }
  }

  @Override
  public void writeState(Element element) {
    setStringAttribute(element, "connection-id", connectionId.id());
    setStringAttribute(element, "default-profile-name", defaultProfileName);
    setStringAttribute(element, "selected-conversation-id", currentConversationId);
    setEnumAttribute(element, "assistant-type", assistantType);
    setEnumAttribute(element, "availability", availability);
    setEnumAttribute(element, "acknowledgement", acknowledgement);

    Element conversationsElement = newElement(element, "conversations");
    for (ChatConversation conversation : conversations.values()) {
        if (isObsoleteConversation(conversation.getId())) continue;

        Element conversationElement = newElement(conversationsElement, "conversation");
        PersistentChatConversation persistentConversation = (PersistentChatConversation) conversation;
        persistentConversation.writeState(conversationElement);
    }
  }

}
