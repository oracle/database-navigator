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

import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.connectionIdAttribute;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

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
  private Map<String, PersistentChatConversation> conversations = new LinkedHashMap<>();
  private String defaultProfileName;
  private boolean listeningForContextChanges = true;
  private String selectedConversationId;

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

  public PersistentChatConversation setCurrentConversation(ChatContext chatContext) {
    PersistentChatConversation currentConversation = new PersistentChatConversation();
    currentConversation.setContext(chatContext);
    selectedConversationId = currentConversation.getId();
    conversations.put(selectedConversationId, currentConversation);
    return currentConversation;
  }

  public synchronized ChatConversation getCurrentConversation() {
    if(selectedConversationId == null || !conversations.containsKey(selectedConversationId)) {
      PersistentChatConversation currentConversation = new PersistentChatConversation();
      selectedConversationId = currentConversation.getId();
      conversations.put(selectedConversationId, currentConversation);
    }
    return conversations.get(selectedConversationId);
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

  public void setDefaultProfile(@Nullable DBAIProfile profile) {
    defaultProfileName = profile == null ? null : profile.getName();
  }

  public String getSelectedProfileName() {
    ChatConversation currentConversation = getCurrentConversation();
    if(currentConversation.getContext()!=null) {
      return currentConversation.getContext().getProfile();
    } else return null;
  }

  public String getSelectedModelName() {
    ChatConversation currentConversation = getCurrentConversation();
    if(currentConversation.getContext()!=null) {
      return currentConversation.getContext().getModel().getId();
    } else return null;
  }

  public PromptAction getSelectedAction() {
    return getCurrentConversation().getContext().getAction();
  }

  public void addMessages(List<PersistentChatMessage> messages) {
   getCurrentConversation().addMessages(messages);
  }

  public ChatContext getChatContext() {
    return getCurrentConversation().getContext();
  }

  public void clearMessages() {
    getCurrentConversation().getMessages().clear();
  }

  @Override
  public void readState(Element element) {
    connectionId = connectionIdAttribute(element, "connection-id");
    defaultProfileName = stringAttribute(element, "default-profile-name");
    selectedConversationId = stringAttribute(element, "selected-conversation-id");
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
      PersistentChatConversation currentConversation = setCurrentConversation(new ChatContext(selectedProfileName, AIModel.forId(selectedModelName), selectedAction, false));
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
    setStringAttribute(element, "selected-conversation-id", selectedConversationId);
    setEnumAttribute(element, "assistant-type", assistantType);
    setEnumAttribute(element, "availability", availability);
    setEnumAttribute(element, "acknowledgement", acknowledgement);

    Element conversationsElement = newElement(element, "conversations");
    for (PersistentChatConversation conversation : conversations.values()) {
      Element conversationElement = newElement(conversationsElement, "conversation");
      conversation.writeState(conversationElement);
    }
  }
}
