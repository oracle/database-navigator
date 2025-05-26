package com.dbn.assistant.chat.window;

import com.dbn.assistant.chat.ChatContext;
import com.dbn.assistant.chat.ChatConversation;
import com.dbn.assistant.chat.ChatInterruptionReason;
import com.dbn.assistant.chat.PersistentChatConversation;
import com.dbn.assistant.chat.ui.SaveOrDiscardConversationDialog;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.assistant.state.AssistantState;
import com.dbn.common.util.Dialogs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Event that handles context changes in chat conversations
 */
public class ContextChangeEvent {
    private final ChatContext oldContext;
    private final ChatContext newContext;
    private final PersistentChatConversation toShowConversation;
    private final boolean isNewConversation;
    private final ChatBoxForm chatBoxForm;

    public ContextChangeEvent(@NotNull ChatContext oldContext,
                              @NotNull ChatContext newContext,
                              @Nullable PersistentChatConversation toShowConversation,
                              boolean isNewConversation,
                              @NotNull ChatBoxForm chatBoxForm) {
        this.oldContext = oldContext;
        this.newContext = newContext;
        this.toShowConversation = toShowConversation;
        this.isNewConversation = isNewConversation;
        this.chatBoxForm = chatBoxForm;
    }

    public void trigger() {
        ChatInterruptionReason changedField = getInterruptionReason(oldContext, newContext, toShowConversation, isNewConversation);
        boolean isOldConversationInteractional = oldContext.isInteractive();
        boolean isNewConversationInteractional = newContext.isInteractive();

        AssistantState state = chatBoxForm.getAssistantState();
        ChatConversation oldConversation = state.getCurrentConversation();

        List<String> titles = chatBoxForm.getConversations().stream().map(PersistentChatConversation::getTitle).collect(Collectors.toList());
        if (isOldConversationInteractional && !oldConversation.isEmpty()) {
            // Old context is an interactive conversation
            if (changedField!=null) {
                Dialogs.show(() -> new SaveOrDiscardConversationDialog(
                                chatBoxForm.getConnection().getProject(), changedField, titles),
                        (dialog, exitCode) -> handleDialogResult(dialog, exitCode, oldConversation, state));
            } else {
                state.getCurrentConversation().setContext(newContext);
            }
        }
        else if (!isOldConversationInteractional && !oldConversation.isEmpty()) {
            // Old context is a non-interactive conversation with messages
            if (isNewConversationInteractional || isNewConversation) {
                Dialogs.show(() -> new SaveOrDiscardConversationDialog(
                                chatBoxForm.getConnection().getProject(), ChatInterruptionReason.PROFILE_SELECTION_CHANGE, titles),
                        (dialog, exitCode) -> handleDialogResult(dialog, exitCode, oldConversation, state));
            } else {
                state.getCurrentConversation().setContext(newContext);
            }
        }
        else {
            // Old context is a normal chat (no messages or non-conversational)
            state.getConversations().remove(oldConversation.getId());
            state.setCurrentConversation(newContext);
            chatBoxForm.initMessages();
            if (toShowConversation != null) {
                chatBoxForm.showConversation(toShowConversation);
            }
        }
    }

    private void handleDialogResult(SaveOrDiscardConversationDialog dialog, int exitCode,
                                    ChatConversation oldConversation, AssistantState state) {
        if (exitCode == 0) {
            // Cancel was selected - stay with current context
            chatBoxForm.updateActionToolbars();
            return;
        }

        if (exitCode == 1) {
            // Discard was selected
            state.getConversations().remove(oldConversation.getId());
            state.setCurrentConversation(newContext);
            if (isNewConversation) chatBoxForm.interruptCurrentConversation();
            chatBoxForm.initMessages();
            if (toShowConversation != null) {
                chatBoxForm.showConversation(toShowConversation);
            }
        }

        if (exitCode == 2) {
            // Save was selected
            state.getCurrentConversation().setTitle(dialog.getConversationTitle());
            state.getCurrentConversation().removeProgress();
            state.getCurrentConversation().getContext().setActive(false);
            state.setCurrentConversation(newContext);
            if (isNewConversation) chatBoxForm.interruptCurrentConversation();
            chatBoxForm.initMessages();
            if (toShowConversation != null) {
                chatBoxForm.showConversation(toShowConversation);
            }
        }
    }

    @Nullable
    private ChatInterruptionReason getInterruptionReason(ChatContext oldContext, ChatContext newContext,
                                                         PersistentChatConversation toShowConversation, boolean isNewConversation) {
        if (toShowConversation != null) return ChatInterruptionReason.HISTORY_CONVERSATION_SELECTION;
        if (isNewConversation) return ChatInterruptionReason.NEW_CONVERSATION_REQUEST;
        if (!oldContext.getProfile().equals(newContext.getProfile())) return ChatInterruptionReason.PROFILE_SELECTION_CHANGE;
        if (oldContext.getModel() != null && !oldContext.getModel().equals(newContext.getModel())) return ChatInterruptionReason.MODEL_SELECTION_CHANGE;
        if (oldContext.getAction() == PromptAction.CHAT && newContext.getAction() != PromptAction.CHAT) {
            return ChatInterruptionReason.ACTION_SELECTION_CHANGE;
        }
        if (oldContext.getAction() != PromptAction.CHAT && newContext.getAction() == PromptAction.CHAT) {
            return ChatInterruptionReason.ACTION_SELECTION_CHANGE;
        }
        // No interruption detected
        return null;
    }
}