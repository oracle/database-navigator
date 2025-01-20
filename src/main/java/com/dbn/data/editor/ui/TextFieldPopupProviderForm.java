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

package com.dbn.data.editor.ui;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.listener.DBNFileEditorManagerListener;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.listener.KeyAdapter;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Keyboard;
import com.dbn.common.util.Context;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.ui.popup.JBPopup;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public abstract class TextFieldPopupProviderForm extends DBNFormBase implements KeyAdapter, TextFieldPopupProvider {
    private final WeakRef<TextFieldWithPopup> editorComponent;
    private final boolean autoPopup;
    private final boolean buttonVisible;

    private boolean enabled = true;
    private JComponent button;
    private JBPopup popup;
    private final Set<AnAction> actions = new HashSet<>();

    protected TextFieldPopupProviderForm(TextFieldWithPopup<?> editorComponent, boolean autoPopup, boolean buttonVisible) {
        super(editorComponent, editorComponent.getProject());
        this.editorComponent = WeakRef.of(editorComponent);
        this.autoPopup = autoPopup;
        this.buttonVisible = buttonVisible;
        ProjectEvents.subscribe(ensureProject(), this, FileEditorManagerListener.FILE_EDITOR_MANAGER, fileEditorManagerListener());
    }

    @NotNull
    private FileEditorManagerListener fileEditorManagerListener() {
        return new DBNFileEditorManagerListener() {
            @Override
            public void whenSelectionChanged(@NotNull FileEditorManagerEvent event) {
                hidePopup();
            }
        };
    }

    public TextFieldWithPopup<?> getEditorComponent() {
        return editorComponent.ensure();
    }

    public JTextField getTextField() {
        return getEditorComponent().getTextField();
    }

    /**
     * Create the popup and return it.
     * If the popup shouldn't show-up for some reason (e.g. empty completion actions),
     * then this method should return null
     */
    @Nullable
    public abstract JBPopup createPopup();

    @Override
    public final String getKeyShortcutDescription() {
        return KeymapUtil.getShortcutsText(getShortcuts());
    }

    @Override
    public final Shortcut[] getShortcuts() {
        return Keyboard.getShortcuts(getKeyShortcutName());
    }

    protected abstract String getKeyShortcutName();

    protected void registerAction(AnAction action) {
        actions.add(action);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.isConsumed()) return;

        for (AnAction action : actions) {
            if (!Keyboard.match(action, e)) continue;

            DataContext dataContext = Context.getDataContext(this);
            ActionManager actionManager = ActionManager.getInstance();
            Presentation templatePresentation = action.getTemplatePresentation();
            Presentation presentation = new Presentation(templatePresentation.getText());
            AnActionEvent actionEvent = new AnActionEvent(null, dataContext, "", presentation, actionManager, 2);
            action.actionPerformed(actionEvent);
            e.consume();
            return;
        }
    }

    @Override
    public void showPopup() {
        if (isShowingPopup()) return;

        TextFieldWithPopup editorComponent = getEditorComponent();
        popup = createPopup();
        if (popup == null) return;

        Disposer.register(TextFieldPopupProviderForm.this, popup);

        JPanel panel = (JPanel) popup.getContent();
        panel.setBorder(Borders.COMPONENT_OUTLINE_BORDER);

        editorComponent.clearSelection();

        if (editorComponent.isShowing()) {
            Point location = editorComponent.getLocationOnScreen();
            location.setLocation(location.getX() + 4, location.getY() + editorComponent.getHeight() + 4);
            popup.showInScreenCoordinates(editorComponent, location);
            //cellEditor.highlight(TextCellEditor.HIGHLIGHT_TYPE_POPUP);
        }
    }

    @Override
    public void hidePopup() {
        if (!isShowingPopup()) return;

        Dispatch.run(true, () -> {
            if (isShowingPopup()) {
                popup.cancel();
                popup = null;
            }
        });
    }

    @Override
    public boolean isShowingPopup() {
        return popup != null && popup.isVisible();
    }


    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return null;
    }
}
