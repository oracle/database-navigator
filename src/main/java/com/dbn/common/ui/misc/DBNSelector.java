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

package com.dbn.common.ui.misc;

import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Mouse;
import com.dbn.common.ui.util.Popups;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.actionSystem.impl.PresentationFactory;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.NlsActions.ActionText;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.plaf.UIResource;
import java.awt.BorderLayout;
import java.awt.Dimension;

import static com.dbn.common.icon.Icons.ACTION_CONTENT_EXPAND;
import static com.dbn.common.ui.util.Popups.popupBuilder;
import static java.awt.event.MouseEvent.BUTTON1;

public final class DBNSelector extends JPanel implements UIResource {
    private ListPopup popup;
    private ActionGroup actionGroup;
    private ActionButton actionButton;

    public DBNSelector(@ActionText String name, ActionGroup actionGroup) {
        this();
        this.actionGroup = actionGroup;
        initAction(name, () -> displayPopup(this));
    }

    public DBNSelector(@ActionText String name, Runnable runnable) {
        this();
        initAction(name, runnable);
    }

    private DBNSelector() {
        super(new BorderLayout());
        setOpaque(false);
    }

    public void bindComponent(JComponent component) {
        Mouse.onMousePress(component, BUTTON1, e ->  displayPopup(component));
    }

    private void initAction(@ActionText String name, Runnable runnable) {
        AnAction action = new DumbAwareAction(name, null, ACTION_CONTENT_EXPAND) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                runnable.run();
            }
        };
        PresentationFactory presentationFactory = new PresentationFactory();
        Presentation presentation = presentationFactory.getPresentation(action);
        actionButton = new ActionButton(
                action, presentation,
                ActionPlaces.TOOLBAR,
                new Dimension(20, 20));
        add(actionButton);
    }

    public DBNSelector withInsets(int insets) {
        actionButton.setBorder(Borders.insetBorder(insets));
        return this;
    }

    public DBNSelector withFocusable(boolean focusable) {
        actionButton.setFocusable(focusable);
        return this;
    }

    private void displayPopup(JComponent source) {
        popup = popupBuilder(actionGroup, this).
                withTitleVisible(false).
                withDisposeCallback(() -> popup = null).
                withMaxRowCount(20).
                withPreselectCondition(a -> false).build();

        Popups.showUnderneathOf(popup, source, 8, 400);
    }

}
