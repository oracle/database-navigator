/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * This software is dual-licensed to you under the Universal Permissive License
 * (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl or Apache License
 * 2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
 * either license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.dbn.assistant.chat.window.ui;

import com.dbn.assistant.DatabaseAssistantManager;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.state.AssistantStateListener;
import com.dbn.common.color.Colors;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.connection.ConnectionId;
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
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

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
        Disposer.register(chatBox, this);

        ProjectEvents.subscribe(getProject(), this, AssistantStateListener.TOPIC, createStateListener());
    }

    /**
     * Creates an {@link AssistantStateListener} that block / unblock input field based on
     * the current state of the assistant
     */
    private AssistantStateListener createStateListener() {
        return (project, connectionId) -> {
            if (!Objects.equals(getConnectionId(), connectionId)) return;

            DatabaseAssistantManager manager = DatabaseAssistantManager.getInstance(project);
            AssistantState assistantState = manager.getAssistantState(connectionId);

            setReadonly(!assistantState.isPromptingAvailable());
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

    private void setReadonly(boolean readonly) {
        Editors.setEditorReadonly(editor, readonly);
        UserInterface.repaint(this);
    }

    private EditorEx createEditor() {
        PlainTextLanguage language = PlainTextLanguage.INSTANCE;
        VirtualFile file = new LightVirtualFile("prompt.txt", language, "");

        Project project = getChatBox().getProject();
        PsiManagerEx psiManager = (PsiManagerEx) PsiManager.getInstance(project);
        FileManager fileManager = psiManager.getFileManager();
        FileViewProvider viewProvider = fileManager.createFileViewProvider(file, true);
        PsiFile psiFile = viewProvider.getPsi(language);
        Document document = Documents.ensureDocument(psiFile);

        EditorEx editor = Editors.createEditor(document, project, file, file.getFileType());
        editor.setEmbeddedIntoDialogWrapper(false);

        JScrollPane scrollPane = editor.getScrollPane();
        Color backgroundColor = Colors.delegate(editor::getBackgroundColor);
        scrollPane.setViewportBorder(Borders.lineBorder(backgroundColor, 2, 4, 2, 4) );
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

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
            if (chatBox.getAssistantState().isPromptingAvailable()) {
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
