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

package com.dbn.execution.common.message.ui.tree;

import com.dbn.common.color.Colors;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.navigation.NavigationInstructions;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.tree.DBNTree;
import com.dbn.common.ui.util.Mouse;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionId;
import com.dbn.database.DatabaseMessage;
import com.dbn.editor.DBContentType;
import com.dbn.editor.EditorProviderId;
import com.dbn.editor.code.SourceCodeEditor;
import com.dbn.editor.console.SQLConsoleEditor;
import com.dbn.execution.common.message.ConsoleMessage;
import com.dbn.execution.common.message.ui.tree.node.CompilerMessageNode;
import com.dbn.execution.common.message.ui.tree.node.StatementExecutionMessageNode;
import com.dbn.execution.compiler.CompilerAction;
import com.dbn.execution.compiler.CompilerMessage;
import com.dbn.execution.explain.result.ExplainPlanMessage;
import com.dbn.execution.statement.StatementExecutionMessage;
import com.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dbn.execution.statement.result.StatementExecutionResult;
import com.dbn.language.common.psi.ExecutablePsiElement;
import com.dbn.vfs.file.DBConsoleVirtualFile;
import com.dbn.vfs.file.DBContentVirtualFile;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.navigation.NavigationInstruction.FOCUS;
import static com.dbn.common.navigation.NavigationInstruction.OPEN;
import static com.dbn.common.navigation.NavigationInstruction.SCROLL;
import static com.dbn.common.navigation.NavigationInstruction.SELECT;

public class MessagesTree extends DBNTree implements Disposable {
    private boolean ignoreSelectionEvent = false;

    public MessagesTree(@NotNull DBNComponent parent) {
        super(parent, new MessagesTreeModel());
        setCellRenderer(new MessagesTreeCellRenderer());
        addTreeSelectionListener(treeSelectionListener);
        addMouseListener(mouseListener);
        addKeyListener(keyListener);
        setRootVisible(false);
        setShowsRootHandles(true);
        setOpaque(false);
        setBackground(Colors.getEditorBackground());
    }

    @Override public void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        for (int i=0; i<getRowCount();i++){
            TreePath treePath = getPathForRow(i);
            if (!isRowSelected(i)) {
                Object lastPathComponent = treePath.getLastPathComponent();
                if (lastPathComponent instanceof MessagesTreeLeafNode) {
                    MessagesTreeLeafNode node = (MessagesTreeLeafNode) lastPathComponent;
                    if (!node.isDisposed() && node.getMessage().isNew()) {
                        Rectangle r = getRowBounds(i);
                        g.setColor(MessagesTreeCellRenderer.HIGHLIGHT_BACKGROUND);
                        g.fillRect(0, r.y, getWidth(), r.height);
                    }
                }
            }
        }
        //super.paintComponent(g);
        if (ui != null) {
            Graphics scratchGraphics = g.create();
            try {
                ui.update(scratchGraphics, this);
            }
            finally {
                scratchGraphics.dispose();
            }
        }
    }

    @Override
    public MessagesTreeModel getModel() {
        return (MessagesTreeModel) super.getModel();
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        Object object = getTreeNode(event);
        if (object == null) return null;
        if (!(object instanceof MessagesTreeLeafNode)) return null;

        MessagesTreeLeafNode node = (MessagesTreeLeafNode) object;
        ConsoleMessage message = node.getMessage();
        if (message instanceof StatementExecutionMessage) {
            StatementExecutionMessage statementExecutionMessage = (StatementExecutionMessage) message;
            DatabaseMessage databaseMessage = statementExecutionMessage.getDatabaseMessage();
            if (databaseMessage == null) return null;
            return databaseMessage.getTooltip();
        }

        return null;
    }

    public void removeMessages(ConnectionId connectionId) {
        getModel().removeMessages(connectionId);
    }

    public void resetMessagesStatus() {
        getModel().resetMessagesStatus();
    }

    public void reset() {
        MessagesTreeModel oldModel = getModel();
        setModel(new MessagesTreeModel());
        Disposer.dispose(oldModel);
    }

    public TreePath addExecutionMessage(StatementExecutionMessage message, NavigationInstructions instructions) {
        TreePath treePath = getModel().addExecutionMessage(message);
        scrollToPath(treePath, instructions);
        return treePath;
    }

    public TreePath addCompilerMessage(CompilerMessage message, NavigationInstructions instructions) {
        TreePath treePath = getModel().addCompilerMessage(message);
        scrollToPath(treePath, instructions);
        return treePath;
    }

    public TreePath addExplainPlanMessage(ExplainPlanMessage message, NavigationInstructions instructions) {
        TreePath treePath = getModel().addExplainPlanMessage(message);
        scrollToPath(treePath, instructions);
        return treePath;
    }

    public void selectCompilerMessage(CompilerMessage message, NavigationInstructions instructions) {
        TreePath treePath = getModel().getTreePath(message);
        scrollToPath(treePath, instructions);
    }

    public void selectExecutionMessage(StatementExecutionMessage message, NavigationInstructions instructions) {
        TreePath treePath = getModel().getTreePath(message);
        scrollToPath(treePath, instructions);
    }

    private void scrollToPath(TreePath treePath, NavigationInstructions instructions) {
        if (treePath != null) {
            Dispatch.run(() -> {
                if (instructions.isScroll()) {
                    scrollPathToVisible(treePath);
                }

                TreeSelectionModel selectionModel = getSelectionModel();
                if (instructions.isSelect()) {
                    try {
                        ignoreSelectionEvent = true;
                        selectionModel.setSelectionPath(treePath);
                    } finally {
                        ignoreSelectionEvent = false;
                    }
                } else {
                    selectionModel.clearSelection();
                }
                if (instructions.isFocus()) {
                    requestFocus();

                } else if (instructions.isOpen()){
                    navigateToCode(treePath.getLastPathComponent(), NavigationInstructions.create(OPEN, SCROLL, FOCUS, SELECT));
                }
            });
        }
    }

/*
    private void focusTree() {
        ExecutionEngineSettings executionEngineSettings = ExecutionEngineSettings.getInstance(project);
        StatementExecutionSettings statementExecutionSettings = executionEngineSettings.getStatementExecutionSettings();
        if (statementExecutionSettings.isFocusResult()) {
            grabFocus();
        }
    }
*/

    private void navigateToCode(Object object, NavigationInstructions instructions) {
        if (object instanceof StatementExecutionMessageNode) {
            StatementExecutionMessageNode execMessageNode = (StatementExecutionMessageNode) object;
            StatementExecutionMessage executionMessage = execMessageNode.getMessage();
            if (!executionMessage.isOrphan()) {
                StatementExecutionResult executionResult = executionMessage.getExecutionResult();
                StatementExecutionProcessor executionProcessor = executionResult.getExecutionProcessor();
                EditorProviderId editorProviderId = executionProcessor.getEditorProviderId();
                VirtualFile virtualFile = executionProcessor.getVirtualFile();
                if (virtualFile == null) return;

                FileEditor fileEditor = executionProcessor.getFileEditor();
                fileEditor = Editors.selectEditor(ensureProject(), fileEditor, virtualFile, editorProviderId, instructions);
                if (fileEditor == null) return;

                ExecutablePsiElement cachedExecutable = executionProcessor.getCachedExecutable();
                if (cachedExecutable == null) return;

                cachedExecutable.navigateInEditor(fileEditor, instructions);
            }
        }
        else if (object instanceof CompilerMessageNode) {
            CompilerMessageNode compilerMessageNode = (CompilerMessageNode) object;
            CompilerMessage compilerMessage = compilerMessageNode.getMessage();

            CompilerAction compilerAction = compilerMessage.getCompilerResult().getCompilerAction();
            if (compilerAction.isSave() || compilerAction.isCompile() || compilerAction.isBulkCompile()) {
                DBEditableObjectVirtualFile databaseFile = compilerMessage.getDatabaseFile();
                if (databaseFile == null) return;

                navigateInObjectEditor(compilerMessage, instructions);
            } else if (compilerAction.isDDL()) {
                VirtualFile virtualFile = compilerAction.getVirtualFile();
                if (virtualFile instanceof DBConsoleVirtualFile) {
                    DBConsoleVirtualFile consoleVirtualFile = (DBConsoleVirtualFile) virtualFile;
                    navigateInConsoleEditor(compilerMessage, consoleVirtualFile, instructions);
                } else if (virtualFile != null) {
                    navigateInScriptEditor(compilerMessage, virtualFile, instructions);
                }
            }
        }
    }

    private void navigateInConsoleEditor(CompilerMessage compilerMessage, DBConsoleVirtualFile virtualFile, NavigationInstructions instructions) {
        if (instructions.isOpen()) {
            Editors.openFileEditor(getProject(), virtualFile, instructions.isFocus());
        }

        FileEditor consoleFileEditor = compilerMessage.getCompilerResult().getCompilerAction().getFileEditor();
        if (consoleFileEditor == null) {
            FileEditorManager editorManager = getFileEditorManager();
            FileEditor[] fileEditors = editorManager.getAllEditors(virtualFile);
            for (FileEditor fileEditor : fileEditors) {
                if (fileEditor instanceof SQLConsoleEditor) {
                    consoleFileEditor = fileEditor;
                }
            }
        }
        navigateInFileEditor(consoleFileEditor, compilerMessage, instructions);
    }

    private void navigateInScriptEditor(CompilerMessage compilerMessage, VirtualFile virtualFile, NavigationInstructions instructions) {
        CompilerAction compilerAction = compilerMessage.getCompilerResult().getCompilerAction();
        FileEditor fileEditor = compilerAction.getFileEditor();
        EditorProviderId editorProviderId = compilerAction.getEditorProviderId();
        fileEditor = Editors.selectEditor(ensureProject(), fileEditor, virtualFile, editorProviderId, instructions);

        navigateInFileEditor(fileEditor, compilerMessage, instructions);
    }

    private void navigateInFileEditor(FileEditor fileEditor, CompilerMessage compilerMessage, NavigationInstructions instructions) {
        CompilerAction compilerAction = compilerMessage.getCompilerResult().getCompilerAction();
        if (fileEditor == null) return;

        Editor editor = Editors.getEditor(fileEditor);
        if (editor == null) return;

        if (!instructions.isOpen() && instructions.isFocus()) {
            Editors.focusEditor(editor);
        }

        if (instructions.isScroll()) {
            int lineShifting = 1;
            Document document = editor.getDocument();
            CharSequence documentText = document.getCharsSequence();
            String objectName = compilerMessage.getObjectName();
            int objectStartOffset = Strings.indexOfIgnoreCase(documentText, objectName, compilerAction.getSourceStartOffset());
            if (objectStartOffset > -1) {
                lineShifting = document.getLineNumber(objectStartOffset);
            }
            navigateInEditor(editor, compilerMessage, lineShifting);
        }
    }

    private void navigateInObjectEditor(CompilerMessage compilerMessage, NavigationInstructions instructions) {
        DBEditableObjectVirtualFile databaseFile = compilerMessage.getDatabaseFile();
        if (isNotValid(databaseFile)) return;

        DBContentVirtualFile contentFile = compilerMessage.getContentFile();
        if (contentFile instanceof DBSourceCodeVirtualFile) {
            CompilerAction compilerAction = compilerMessage.getCompilerResult().getCompilerAction();
            FileEditor objectFileEditor = compilerAction.getFileEditor();
            EditorProviderId editorProviderId = compilerAction.getEditorProviderId();
            if (editorProviderId == null) {
                DBContentType contentType = compilerMessage.getContentType();
                switch (contentType) {
                    case CODE: editorProviderId = EditorProviderId.CODE; break;
                    case CODE_SPEC: editorProviderId = EditorProviderId.CODE_SPEC;  break;
                    case CODE_BODY: editorProviderId = EditorProviderId.CODE_BODY; break;
                }
            }
            Project project = ensureProject();
            objectFileEditor = Editors.selectEditor(project, objectFileEditor, databaseFile, editorProviderId, instructions);

            if (objectFileEditor instanceof SourceCodeEditor) {
                SourceCodeEditor codeEditor = (SourceCodeEditor) objectFileEditor;
                Editor editor = Editors.getEditor(codeEditor);
                if (editor != null) {
                    if (instructions.isScroll()) {
                        Document document = editor.getDocument();
                        int lineShifting = document.getLineNumber(codeEditor.getHeaderEndOffset());
                        navigateInEditor(editor, compilerMessage, lineShifting);
                    }
                    VirtualFile virtualFile = Documents.getVirtualFile(editor);
                    if (virtualFile != null) {
                        OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, virtualFile);
                        codeEditor.navigateTo(openFileDescriptor);
                    }
                }
            }
        }

    }

    private static void navigateInEditor(Editor editor, CompilerMessage compilerMessage, int lineShifting) {
        Document document = editor.getDocument();
        if (document.getLineCount() > compilerMessage.getLine() + lineShifting) {
            int lineStartOffset = document.getLineStartOffset(compilerMessage.getLine() + lineShifting);
            int newCaretOffset = lineStartOffset + compilerMessage.getPosition();
            if (document.getTextLength() > newCaretOffset) {
                editor.getCaretModel().moveToOffset(newCaretOffset);

                String identifier = compilerMessage.getSubjectIdentifier();
                SelectionModel selectionModel = editor.getSelectionModel();
                selectionModel.removeSelection();
                if (identifier != null) {
                    int lineEndOffset = document.getLineEndOffset(compilerMessage.getLine() + lineShifting);
                    CharSequence lineText = document.getCharsSequence().subSequence(lineStartOffset, lineEndOffset);
                    int selectionOffsetInLine = Strings.indexOfIgnoreCase(lineText, identifier, compilerMessage.getPosition());
                    if (selectionOffsetInLine > -1) {
                        int selectionOffset = selectionOffsetInLine + lineStartOffset;
                        selectionModel.setSelection(selectionOffset, selectionOffset + identifier.length());
                    }
                }
                editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
            }
        }
    }


    private FileEditorManager getFileEditorManager() {
        Project project = ensureProject();
        return FileEditorManager.getInstance(project);
    }

    /*********************************************************
     *                   TreeSelectionListener               *
     *********************************************************/
    private final TreeSelectionListener treeSelectionListener = event -> {
        if (event.isAddedPath() && !ignoreSelectionEvent) {
            Object object = event.getPath().getLastPathComponent();
            navigateToCode(object, NavigationInstructions.create(OPEN, SCROLL));
            //grabFocus();
        }
    };


    /*********************************************************
     *                        MouseListener                  *
     *********************************************************/
    private final MouseListener mouseListener = Mouse.listener().onClick(e -> {
        if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() > 1) {
            TreePath selectionPath = getSelectionPath();
            if (selectionPath != null) {
                Object value = selectionPath.getLastPathComponent();
                navigateToCode(value, NavigationInstructions.create(OPEN, FOCUS, SCROLL));
            }
        }
    });

    /*********************************************************
     *                        KeyListener                    *
     *********************************************************/
    private final KeyListener keyListener = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER ) {
                TreePath selectionPath = getSelectionPath();
                if (selectionPath != null) {
                    Object value = selectionPath.getLastPathComponent();
                    navigateToCode(value, NavigationInstructions.create(OPEN, FOCUS, SCROLL));
                }
            }
        }
    };

}
