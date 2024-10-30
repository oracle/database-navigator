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

package com.dbn.assistant.chat.message.ui;

import com.dbn.assistant.chat.message.ChatMessageSection;
import com.dbn.assistant.chat.message.action.CopyContentAction;
import com.dbn.common.color.Colors;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Viewers;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.intellij.lang.Language;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

import static javax.swing.JLayeredPane.DRAG_LAYER;

/**
 * Specialized viewer for AI responses containing qualified code sections
 *
 * @author Dan Cioca (Oracle)
 */
@Getter
public class ChatMessageCodeViewer extends JPanel implements Disposable {
    private final EditorEx viewer;

    private ChatMessageCodeViewer(EditorEx viewer) {
        super(new BorderLayout());
        this.viewer = viewer;
        setOpaque(false);
        setBorder(JBUI.Borders.empty(10));
        add(viewer.getComponent(), BorderLayout.CENTER);

        initActionToolbar();
    }

    private void initActionToolbar() {
        JPanel actionPanel = new JPanel();
        actionPanel.setOpaque(false);
        String content = viewer.getDocument().getText();
        ActionToolbar actionToolbar = Actions.createActionToolbar(actionPanel, "", true, new CopyContentAction(content));
        JComponent component = actionToolbar.getComponent();
        component.setOpaque(false);
        component.setBorder(Borders.EMPTY_BORDER);
        actionPanel.add(component, BorderLayout.NORTH);

        JComponent viewerComponent = viewer.getComponent();
        UserInterface.visitRecursively(viewerComponent,JLayeredPane.class, p -> p.add(actionPanel, DRAG_LAYER));
    }

    public static ChatMessageCodeViewer create(ConnectionHandler connection, ChatMessageSection section){
        EditorEx viewer = createViewer(connection, section);
        if (viewer == null) return null;

        return new ChatMessageCodeViewer(viewer);
    }


    @Nullable
    private static EditorEx createViewer(ConnectionHandler connection, ChatMessageSection section) {
        Language language = section.getLanguage();
        if (language == null) return null;

        LanguageFileType fileType = language.getAssociatedFileType();
        String fileName = "ai_preview_file." + (fileType == null ? "txt" : fileType.getDefaultExtension());
        VirtualFile file = new LightVirtualFile(fileName, language, section.getContent());

        Project project = connection.getProject();
        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
        contextManager.setConnection(file, connection);

        PsiManagerEx psiManager = (PsiManagerEx) PsiManager.getInstance(project);
        FileManager fileManager = psiManager.getFileManager();
        FileViewProvider viewProvider = fileManager.createFileViewProvider(file, true);
        PsiFile psiFile = viewProvider.getPsi(language);
        if (psiFile == null) return null;

        Document document = Documents.getDocument(psiFile);
        if (document == null) return null;

        EditorEx viewer = Viewers.createViewer(document, project, file, file.getFileType());
        viewer.setEmbeddedIntoDialogWrapper(false);
        //Editors.initEditorHighlighter(viewer, language, connection);

        JScrollPane viewerScrollPane = viewer.getScrollPane();
        viewerScrollPane.setViewportBorder(Borders.lineBorder(Colors.delegate(() -> viewer.getBackgroundColor()), 8));
        viewerScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        viewerScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        viewerScrollPane.setBorder(null);
        //viewer.getComponent().setBorder(JBUI.Borders.empty(10));

        EditorSettings settings = viewer.getSettings();
        settings.setFoldingOutlineShown(false);
        settings.setLineMarkerAreaShown(false);
        settings.setLineNumbersShown(false);
        settings.setVirtualSpace(false);
        settings.setDndEnabled(false);
        settings.setRightMarginShown(false);
        settings.setCaretRowShown(false);
        settings.setUseSoftWraps(true);
        settings.setAdditionalLinesCount(0);
        settings.setAutoCodeFoldingEnabled(false);

        return viewer;
    }

    @Override
    public void dispose() {
        Editors.releaseEditor(viewer);
    }
}
