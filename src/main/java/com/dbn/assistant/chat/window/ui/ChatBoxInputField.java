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

package com.dbn.assistant.chat.window.ui;

import com.dbn.assistant.chat.ChatAvailability;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.state.AssistantStateListener;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionStatusListener;
import com.dbn.connection.SessionId;
import com.dbn.object.event.ObjectChangeListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import java.awt.BorderLayout;

import static com.dbn.language.common.psi.PsiUtil.getFileManager;
import static com.dbn.object.type.DBObjectType.AI_PROFILE;

/**
 * Input field used for the chat-box user prompt
 *
 * @author Dan Cioca (Oracle)
 */
public class ChatBoxInputField extends JPanel implements Disposable {
    private final EditorEx editor;
    private final WeakRef<ChatBoxForm> chatBox;

    public ChatBoxInputField(ChatBoxForm chatBox) {
        super(new BorderLayout());
        this.chatBox = WeakRef.of(chatBox);
        this.editor = createEditor();
        add(editor.getComponent(), BorderLayout.CENTER);
        setBorder(Borders.insetBorder(0, 8, 8, 8));
        Disposer.register(chatBox, this);

        ProjectEvents.subscribe(getProject(), this, AssistantStateListener.TOPIC, createStateListener());
        ProjectEvents.subscribe(getProject(), this, ObjectChangeListener.TOPIC, createObjectChangeListener());
        ProjectEvents.subscribe(getProject(), this, ConnectionStatusListener.TOPIC, createConnectionListener());
    }

    /**
     * Creates an {@link AssistantStateListener} that block / unblock input field based on
     * the current state of the assistant
     */
    private AssistantStateListener createStateListener() {
        return (project, connectionId) -> {
            if (connectionId != getConnectionId()) return;
            refreshComponentState();
        };
    }

    private ObjectChangeListener createObjectChangeListener() {
        return e -> {
            if (!e.matches(getConnectionId())) return;
            if (!e.matches(AI_PROFILE)) return;

            refreshComponentState();
        };
    }

    private ConnectionStatusListener createConnectionListener() {
        return (connectionId, sessionId) -> {
            if (connectionId != getConnectionId()) return;
            if (sessionId != SessionId.ASSISTANT) return;

            refreshComponentState();
        };
    }

    private void refreshComponentState() {
        ChatBoxForm chatBox = getChatBox();
        AssistantState state = chatBox.getAssistantState();
        ChatAvailability availability = state.getChatAvailability();

        boolean readonly = availability != ChatAvailability.AVAILABLE;
        String readonlyHint = computeReadonlyHint(availability);

        Editors.setEditorReadonly(editor, readonly);
        Editors.setEditorReadonlyHint(editor, readonlyHint);

        UserInterface.repaint(this);
    }

    @Nullable
    private String computeReadonlyHint(ChatAvailability availability) {
        return switch (availability) {
            case AVAILABLE -> null;
            case BUSY_QUERYING -> "Assistant is processing your request...";
            case BUSY_INITIALIZING -> "Assistant is initializing...";
            case INACTIVE_CHAT_SELECTED -> "This chat is no longer active";
            case NO_PROFILE_AVAILABLE -> "No profiles available for this connection. Please setup profiles to continue";
            case NO_PROFILE_SELECTED -> "No profile selected. Please select a profile to continue";
            case DISABLED_PROFILE_SELECTED -> "The selected profile is disabled. Please select an active profile to continue";
            default -> null;
        };

    }

    private Project getProject() {
        return getChatBox().getProject();
    }

    private ConnectionId getConnectionId() {
        return getChatBox().getConnection().getConnectionId();
    }



    @Override
    public void requestFocus() {
        editor.getContentComponent().requestFocus();
    }

    public String getText() {
        return editor.getDocument().getText();
    }

    private ChatBoxForm getChatBox() {
        return chatBox.ensure();
    }

    public void setText(String text) {
        editor.getDocument().setText(text);
    }

    public String getAndClearText() {
        DocumentEx document = editor.getDocument();
        String text = document.getText().trim();
        Documents.setText(document, "");
        return text;
    }

    private EditorEx createEditor() {
        PlainTextLanguage language = PlainTextLanguage.INSTANCE;
        VirtualFile file = new LightVirtualFile("prompt.txt", language, "");

        Project project = getChatBox().ensureProject();
        FileManager fileManager = getFileManager(project);
        FileViewProvider viewProvider = fileManager.createFileViewProvider(file, true);
        PsiFile psiFile = viewProvider.getPsi(language);
        Document document = Documents.ensureDocument(psiFile);

        EditorEx editor = Editors.createEditor(document, project, file, file.getFileType());
        //editor.setEmbeddedIntoDialogWrapper(false); TODO quick-fix check why it does not grab focus if this is set to false
        editor.setBorder(Borders.EMPTY_BORDER);
        Editors.updateEditorScrollPane(editor);

        document.addDocumentListener(new EnterKeyInterceptor());

        EditorSettings settings = editor.getSettings();
        settings.setFoldingOutlineShown(false);
        settings.setLineMarkerAreaShown(false);
        settings.setLineNumbersShown(false);
        settings.setVirtualSpace(false);
        settings.setDndEnabled(false);
        settings.setAdditionalLinesCount(1);
        settings.setRightMarginShown(false);
        settings.setCaretRowShown(false);
        settings.setUseSoftWraps(true);
        settings.setAdditionalLinesCount(2);

        return editor;

    }

    private class EnterKeyInterceptor implements DocumentListener {
        @Override
        public void beforeDocumentChange(@NotNull DocumentEvent event) {
            Document document = event.getDocument();

            CharSequence newFragment = event.getNewFragment();
            if (newFragment.length() == 0) return;
            if (newFragment.charAt(0) != '\n') return;

            CharSequence text = document.getImmutableCharSequence();
            for (int i = event.getOffset(); i < text.length(); i++) {
                if (!Character.isWhitespace(text.charAt(i))) return;
            }

            ChatBoxForm chatBox = getChatBox();
            AssistantState state = chatBox.getAssistantState();
            ChatAvailability availability = state.getChatAvailability();

            if (availability == ChatAvailability.AVAILABLE) {
                chatBox.submitPrompt();
            } else {
                Documents.setText(document, text.toString().trim());
            }
        }
    }

    @Override
    public void dispose() {
        Editors.releaseEditor(editor);
    }
}
