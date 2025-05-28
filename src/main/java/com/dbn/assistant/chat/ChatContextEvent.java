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

import com.dbn.assistant.state.AssistantState;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event that handles context changes in chat conversations
 */
@Getter
@Setter
public class ChatContextEvent {
    private final ChatContext currentContext;
    private final ChatContext targetContext;
    private final String targetConversationId;
    private boolean newConversationRequest;

    public ChatContextEvent(@NotNull ChatContext currentContext,
                            @NotNull ChatContext targetContext,
                            @Nullable String targetConversationId,
                            boolean newConversationRequest) {
        this.currentContext = currentContext;
        this.targetContext = targetContext;
        this.targetConversationId = targetConversationId;
        this.newConversationRequest = newConversationRequest;
    }

    public boolean isConversationOpenRequest() {
        return targetConversationId != null;
    }

    @Nullable
    public ChatInterruptionReason evaluateInterruption(AssistantState state) {
        ChatConversation currentConversation = state.getCurrentConversation();

        // if current is a previously interactive persistent conversation, signal no interruption
        if (currentConversation.isInteractive() && currentConversation.isPersisted()) return null;

        // if the current conversation is empty, signal no interruption
        if (currentConversation.isEmpty()) return null;

        if (newConversationRequest) return ChatInterruptionReason.NEW_CONVERSATION_REQUEST;
        if (targetConversationId != null) return ChatInterruptionReason.HISTORY_CONVERSATION_SELECTION;

        // if the current conversation is non-interactive, switching to a non-interactive context, signal no interruption
        if (!currentContext.isInteractive() && !targetContext.isInteractive()) return null;

        if (currentContext.isProfileSwitch(targetContext)) return ChatInterruptionReason.PROFILE_SELECTION_CHANGE;
        if (currentContext.isModelSwitch(targetContext)) return ChatInterruptionReason.MODEL_SELECTION_CHANGE;
        if (currentContext.isInterruptingActionSwitch(targetContext)) return ChatInterruptionReason.ACTION_SELECTION_CHANGE;

        // No interruption detected
        return null;
    }
}