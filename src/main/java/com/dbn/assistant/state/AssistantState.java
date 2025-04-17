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

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.connectionIdAttribute;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setBooleanAttribute;
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
  private List<PersistentChatMessage> messages = new ArrayList<>();
  private List<PersistentChatConversation> conversations = new ArrayList<>();
  private PromptAction selectedAction = PromptAction.SHOW_SQL;
  private String defaultProfileName;
  private String selectedProfileName;
  private String selectedModelName;
  private boolean listeningForContextChanges = true;
  private boolean conversation;
  private PersistentChatConversation currentConversation;

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

  public void setSelectedProfile(@Nullable DBAIProfile profile) {
    selectedProfileName = profile == null ? null : profile.getName();
    conversation = profile != null && profile.isConversation();
  }
  public void setSelectedModel(@Nullable AIModel model) {
    selectedModelName = model == null ? null : model.getName();
  }
  public void setDefaultProfile(@Nullable DBAIProfile profile) {
    defaultProfileName = profile == null ? null : profile.getName();
  }

  public void addMessages(List<PersistentChatMessage> messages) {
    if(currentConversation != null){
      this.currentConversation.addMessages(messages);
    } else {
      this.messages.addAll(messages);
    }
  }

  public ChatContext getChatContext() {
    return new ChatContext(selectedProfileName, AIModel.forId(selectedModelName), selectedAction, conversation);
  }

//  public void applyChatContext(ChatContext context) {
//    setSelectedProfileName(context.getProfile());
//    setSelectedModelName(context.getModel().getName());
//    setSelectedAction(context.getAction());
//  }

  public void clearMessages() {
    messages.clear();
  }

  @Override
  public void readState(Element element) {
    connectionId = connectionIdAttribute(element, "connection-id");
    defaultProfileName = stringAttribute(element, "default-profile-name");
    selectedProfileName = stringAttribute(element, "selected-profile-name");
    selectedModelName = stringAttribute(element, "selected-model-name");
    conversation = booleanAttribute(element, "is-conversation", false);
    assistantType = enumAttribute(element, "assistant-type", assistantType);
    selectedAction = enumAttribute(element, "selected-action", selectedAction);
    availability = enumAttribute(element, "availability", availability);
    acknowledgement = enumAttribute(element, "acknowledgement", acknowledgement);

    Element messagesElement = element.getChild("messages");
    List<Element> messageElements = childrenOf(messagesElement);
    for (Element messageElement : messageElements) {
      PersistentChatMessage message = new PersistentChatMessage();
      message.readState(messageElement);
      messages.add(message);
    }

    Element conversationsElement = element.getChild("conversations");
    List<Element> conversationElements = childrenOf(conversationsElement);
    for (Element conversationElement : conversationElements) {
      PersistentChatConversation conversation = new PersistentChatConversation();
      conversation.readState(conversationElement);
      conversations.add(conversation);
    }
  }

  @Override
  public void writeState(Element element) {
    setStringAttribute(element, "connection-id", connectionId.id());
    setStringAttribute(element, "default-profile-name", defaultProfileName);
    setStringAttribute(element, "selected-profile-name", selectedProfileName);
    setStringAttribute(element, "selected-model-name", selectedModelName);
    setBooleanAttribute(element, "is-conversation", conversation);
    setEnumAttribute(element, "assistant-type", assistantType);
    setEnumAttribute(element, "selected-action", selectedAction);
    setEnumAttribute(element, "availability", availability);
    setEnumAttribute(element, "acknowledgement", acknowledgement);

    Element messagesElement = newElement(element, "messages");
    for (PersistentChatMessage message : messages) {
      Element messageElement = newElement(messagesElement, "message");
      message.writeState(messageElement);
    }

    Element conversationsElement = newElement(element, "conversations");
    for (PersistentChatConversation conversation : conversations) {
      Element conversationElement = newElement(conversationsElement, "conversation");
      conversation.writeState(conversationElement);
    }
  }
}
