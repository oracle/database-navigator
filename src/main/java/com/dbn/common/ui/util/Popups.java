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

package com.dbn.common.ui.util;

import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Context;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.ui.popup.IPopupChooserBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.popup.list.ListPopupImpl;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.SwingConstants;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.util.List;
import java.util.function.Predicate;

public class Popups {
    public static void showUnderneathOf(@NotNull JBPopup popup, @NotNull Component sourceComponent, int verticalShift, int maxHeight) {
        JComponent popupContent = popup.getContent();
        Dimension preferredSize = popupContent.getPreferredSize();
        int width = Math.max((int) preferredSize.getWidth(), sourceComponent.getWidth());
        int height = (int) Math.min(maxHeight, preferredSize.getHeight());

        if (popup instanceof ListPopupImpl) {
            ListPopupImpl listPopup = (ListPopupImpl) popup;
            JList list = listPopup.getList();
            int listHeight = (int) list.getPreferredSize().getHeight();
            if (listHeight > height) {
                height = Math.min(maxHeight, listHeight);
            }
        }

        popupContent.setPreferredSize(new Dimension(width, height));

        popup.show(new RelativePoint(sourceComponent, new Point(0, sourceComponent.getHeight() + verticalShift)));
    }

    public static <T> void showCompletionPopup(
            JComponent toolbarComponent,
            List<T> elements,
            String title,
            JTextComponent textField,
            String adText) {

        JBPopupFactory popupFactory = JBPopupFactory.getInstance();
        IPopupChooserBuilder<T> builder = popupFactory.createPopupChooserBuilder(elements);
        if (title != null) builder.setTitle(title);

        JBPopup popup = builder.
                setMovable(false).
                setResizable(false).
                setRequestFocus(true).
                setItemChosenCallback(e -> {
                    if (e == null) return;

                    textField.setText(e.toString());
                    IdeFocusManager.getGlobalInstance().requestFocus(textField, false);
                }).
                createPopup();

        if (adText != null) {
            popup.setAdText(adText, SwingConstants.LEFT);
        }

        if (toolbarComponent != null) {
            popup.showUnderneathOf(toolbarComponent);
        }
        else {
            popup.showUnderneathOf(textField);
        }
    }

    public static void showActionsPopup(String title, Object context, List<? extends AnAction> actions, Predicate<AnAction> selected) {
        Dispatch.run(() -> { //AWT events are not allowed inside write action
            ActionGroup actionGroup = new DefaultActionGroup(actions);

            DataContext dataContext = Context.getDataContext(context);
            ListPopup popupBuilder = JBPopupFactory.getInstance().createActionGroupPopup(
                    title,
                    actionGroup,
                    dataContext,
                    JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                    true,
                    null,
                    1000,
                    action -> selected.test(action),
                    null);

            popupBuilder.showInBestPositionFor(dataContext);
        });
    }
}
