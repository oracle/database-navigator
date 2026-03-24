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

package com.dbn.assistant.chat.window.ui;

import com.dbn.assistant.AssistantContext;
import com.dbn.assistant.AssistantType;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.ui.CardLayouts;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBinding;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Component;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.ui.CardLayouts.addBlankCard;
import static com.dbn.common.ui.CardLayouts.isBlankCard;
import static com.dbn.common.ui.CardLayouts.visibleCardId;

public class ChatBoxFormContainer extends JPanel {
    private final Map<AssistantContext, ChatBoxForm> chatBoxes = new ConcurrentHashMap<>();

    public ChatBoxFormContainer() {
        setLayout(new CardLayout());
        addBlankCard(this);
    }

    public void focusInputField() {
        String identifier = visibleCardId(this);
        Component component = CardLayouts.getCard(this, identifier);
        if (component == null) return;

        ChatBoxForm chatBoxForm = DBNFormBinding.getForm(component);
        if (chatBoxForm == null) return;

        chatBoxForm.focusInputField();
    }

    public void showBlankCard() {
        CardLayouts.showBlankCard(this);
    }

    public void addCard(ChatBoxForm chatBox) {
        ConnectionId connectionId = chatBox.getConnectionId();
        AssistantType assistantType = chatBox.getAssistantType();
        AssistantContext context = new AssistantContext(connectionId, assistantType);

        CardLayouts.addCard(this, chatBox, context);
    }

    public void showCard(ConnectionId connectionId, AssistantType assistantType) {
        AssistantContext context = new AssistantContext(connectionId, assistantType);
        CardLayouts.showCard(this, context);
    }

    public void removeCards(ConnectionId connectionId) {
        List<String> cardIds = CardLayouts.cardIds(this);
        for (String cardId : cardIds) {
            removeCard(connectionId, cardId);
        }
    }

    private void removeCard(ConnectionId connectionId, String cardId) {
        if (CardLayouts.isBlankCard(cardId)) return;

        AssistantContext context = AssistantContext.fromIdentifier(cardId);
        if (!context.getConnectionId().equals(connectionId)) return;

        Component component = CardLayouts.removeCard(this, context);
        if (component == null) return;

        DBNForm form = DBNFormBinding.getForm(component);
        Disposer.dispose(form);
    }

    public ChatBoxForm createCard(ConnectionId connectionId, AssistantType assistantType) {
        ConnectionHandler connection = ConnectionHandler.ensure(connectionId);
        ChatBoxForm chatBox = new ChatBoxForm(connection, assistantType);
        addCard(chatBox);
        return chatBox;
    }

    public AssistantContext getCurrentContext() {
        String identifier = visibleCardId(this);
        if (identifier == null) return null;
        if (isBlankCard(identifier)) return null;

        return new AssistantContext(identifier);
    }

    public boolean matchesCurrentContext(ConnectionId connectionId, AssistantType assistantType) {
        AssistantContext currentContext = getCurrentContext();
        return currentContext != null && currentContext.matches(connectionId, assistantType);
    }

    public boolean initCard(@Nullable ConnectionId connectionId, @Nullable AssistantType assistantType) {
        ChatBoxForm chatBoxForm = initForm(connectionId, assistantType);
        if (chatBoxForm == null) {
            showBlankCard();
            return false;
        }

        showCard(connectionId, assistantType);
        return true;
    }

    private ChatBoxForm initForm(@Nullable ConnectionId connectionId, @Nullable AssistantType assistantType) {
        if (connectionId == null) return null;
        if (assistantType == null) return null;

        ConnectionHandler connection = ConnectionHandler.get(connectionId);
        if (connection == null) return null;

        AssistantContext assistantContext = new AssistantContext(connectionId, assistantType);
        return chatBoxes.computeIfAbsent(assistantContext, id ->  createCard(connectionId, assistantType));
    }
}
