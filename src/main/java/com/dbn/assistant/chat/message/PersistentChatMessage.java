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

import com.dbn.common.message.MessageType;
import com.dbn.common.state.PersistentStateElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.readCdata;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.options.setting.Settings.writeCdata;

/**
 * This class is for message elements that will be in the chat
 *
 * @author Ayoub Aarrasse (Oracle)
 */
@Getter
@Setter
@NoArgsConstructor
public class PersistentChatMessage extends ChatMessage implements PersistentStateElement {

  /**
   * Creates a new ChatMessage
   *
   * @param type the message type (relevant for SYSTEM messages)
   * @param content the message content
   * @param author  the author of the message
   * @param context the context in which the chat message was produced
   */
  public PersistentChatMessage(MessageType type, String content, AuthorType author, ChatMessageContext context) {
    super(type, content, author, context);
  }

  @Override
  public void readState(Element element) {
    id = stringAttribute(element, "id");
    type = enumAttribute(element, "type", type);
    author = enumAttribute(element, "author", AuthorType.class);

    Element contentElement = element.getChild("content");
    content = readCdata(contentElement);

    Element contextElement = element.getChild("context");
    context = new ChatMessageContext();
    context.readState(contextElement);
  }

  @Override
  public void writeState(Element element) {
    setStringAttribute(element, "id", id);
    setEnumAttribute(element, "type", type);
    setEnumAttribute(element, "author", author);

    Element contentElement = newElement(element,"content");
    writeCdata(contentElement, content);

    Element contextElement = newElement(element,"context");
    context.writeState(contextElement);
  }

}

