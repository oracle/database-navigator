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

package com.dbn.assistant.chat;

import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.chat.message.AuthorType;
import com.dbn.assistant.state.AssistantState;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event that handles context changes in chats
 */
@Getter
@Setter
public final class ChatContextEvent {
    private final ChatContext currentContext;
    private final ChatContext targetContext;
    private final String targetChatId;
    private boolean newChatRequest;

    public ChatContextEvent(
            @NotNull ChatContext currentContext,
            @NotNull ChatContext targetContext,
            @Nullable String targetChatId,
            boolean newChatRequest) {

        this.currentContext = currentContext;
        this.targetContext = targetContext;
        this.targetChatId = targetChatId;
        this.newChatRequest = newChatRequest;
    }

    public boolean isChatOpenRequest() {
        return targetChatId != null;
    }

    @Nullable
    public ChatInterruptionReason evaluateInterruption(AssistantState state) {
        Chat currentChat = state.getCurrentChat();

        // if current is a previously interactive persistent chat, signal no interruption
        if (currentChat.isInteractive() && currentChat.isPersisted()) return null;

        // if the current chat is empty or has no agent messages, signal no interruption
        if (currentChat.isEmpty()) return null;
        if (currentChat.countMessages(AuthorType.AGENT) == 0) return null;

        if (newChatRequest) return ChatInterruptionReason.NEW_CHAT_REQUEST;
        if (targetChatId != null) return ChatInterruptionReason.HISTORY_CHAT_SELECTION;

        // if the current chat is non-interactive, switching to a non-interactive context, signal no interruption
        if (!currentContext.isInteractive() && !targetContext.isInteractive()) return null;

        if (currentContext.isProfileSwitch(targetContext)) return ChatInterruptionReason.PROFILE_SELECTION_CHANGE;
        if (currentContext.isProviderSwitch(targetContext)) return ChatInterruptionReason.PROVIDER_SELECTION_CHANGE;
        if (currentContext.isModelSwitch(targetContext)) return ChatInterruptionReason.MODEL_SELECTION_CHANGE;
        if (currentContext.isActionSwitch(targetContext)) return ChatInterruptionReason.ACTION_SELECTION_CHANGE;

        // No interruption detected
        return null;
    }
}