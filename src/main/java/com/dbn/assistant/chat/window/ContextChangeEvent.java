package com.dbn.assistant.chat.window;

import com.dbn.assistant.chat.ChatContext;
import com.dbn.assistant.chat.ChatConversation;
import com.dbn.assistant.chat.PersistentChatConversation;
import com.dbn.assistant.chat.ui.SaveOrDiscardConversationDialog;
import com.dbn.assistant.state.AssistantState;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionRef;
import com.dbn.common.thread.Dispatch;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        String changedField = isConversationInterruption(oldContext, newContext, toShowConversation, isNewConversation);
        boolean isOldConversationInteractional = oldContext.isInteractive();
        boolean isNewConversationInteractional = newContext.isInteractive();

        AssistantState state = chatBoxForm.getAssistantState();
        ChatConversation oldConversation = state.getCurrentConversation();

        if (isOldConversationInteractional && !oldConversation.getMessages().isEmpty()) {
            // Old context is an interactive conversation
            if (!changedField.isEmpty()) {
                Dialogs.show(() -> new SaveOrDiscardConversationDialog(
                                chatBoxForm.getConnection().getProject(), changedField),
                        (dialog, exitCode) -> handleDialogResult(dialog, exitCode, oldConversation, state));
            } else {
                state.getCurrentConversation().setContext(newContext);
            }
        }
        else if (!isOldConversationInteractional && !oldConversation.getMessages().isEmpty()) {
            // Old context is a non-interactive conversation with messages
            if (isNewConversationInteractional || isNewConversation) {
                Dialogs.show(() -> new SaveOrDiscardConversationDialog(
                                chatBoxForm.getConnection().getProject(), "profile"),
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

    private String isConversationInterruption(ChatContext oldContext, ChatContext newContext,
                                              PersistentChatConversation toShowConversation, boolean isNewConversation) {
        if (toShowConversation != null) return "conversation history";
        if (isNewConversation) return "new conversation";
        if (!oldContext.getProfile().equals(newContext.getProfile())) return "profile";
        if (oldContext.getModel() != null && !oldContext.getModel().equals(newContext.getModel())) return "model";
        if (oldContext.getAction() == PromptAction.CHAT && newContext.getAction() != PromptAction.CHAT) {
            return "conversation type";
        }
        if (oldContext.getAction() != PromptAction.CHAT && newContext.getAction() == PromptAction.CHAT) {
            return "conversation type";
        }
        // No interruption detected
        return "";
    }
}