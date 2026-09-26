/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.common.ui.misc;

import com.dbn.common.color.Colors;
import com.dbn.common.ui.util.Mouse;
import com.dbn.common.ui.util.Popups;
import com.dbn.common.ui.util.UserInterface;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.ui.popup.ListPopup;
import org.jetbrains.annotations.Nullable;

import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseListener;

import static com.dbn.common.util.Conditional.when;

public abstract class DBNActionComboBox<T> extends JComboBox<T> {
    private ListPopup popup;

    protected DBNActionComboBox() {
        this(new DBNComboBoxModel<>());
    }

    protected DBNActionComboBox(@Nullable ComboBoxModel<T> model) {
        super(model);
        Mouse.removeMouseListeners(this);

        MouseListener mouseListener = Mouse
                .listener()
                .onPress(e -> when(isEnabled(), this::showPopup));

        addMouseListener(mouseListener);
        for (Component component : getComponents()) {
            component.addMouseListener(mouseListener);
        }
        setBackground(Colors.getTextFieldBackground());
    }

    @Override
    public void setBorder(Border border) {
        super.setBorder(border);
    }

    @Override
    public void setBackground(Color background) {
        super.setBackground(background);
        ComboBoxEditor editor = getEditor();
        if (editor != null) {
            editor.getEditorComponent().setBackground(background);
        }
    }

    @Override
    public void setPopupVisible(boolean visible) {
        if (visible && !isPopupVisible()) {
            showPopup();
        }
    }

    @Override
    public boolean isPopupVisible() {
        return popup != null;
    }

    public void showPopup() {
        ActionGroup actionGroup = createActionGroup();

        JLabel label = UserInterface.getComponentLabel(this);
        String title = label == null ? null : label.getText();
        popup = Popups.popupBuilder(actionGroup, this).
                withTitle(title).
                withTitleVisible(false).
                withHint(getHint()).
                withMaxRowCount(10).
                withSpeedSearch().
                withDisposeCallback(this::disposePopup).
                withPreselectCondition(this::preselectAction).
                build();

        Popups.showUnderneathOf(popup, this, 3, 200);
    }

    protected abstract ActionGroup createActionGroup();

    protected String getHint() {
        return null;
    }

    protected boolean preselectAction(AnAction action) {
        return false;
    }

    protected void disposePopup() {
        popup = null;
        UserInterface.repaintAndFocus(this);
    }

    protected void closePopup() {
        if (popup != null) {
            popup.cancel();
        }
    }
}
