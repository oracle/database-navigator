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

package com.dbn.common.action;

import com.dbn.common.ref.WeakRef;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.intellij.ui.popup.list.ListPopupImpl;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;

public abstract class ComboBoxAction
        extends com.intellij.openapi.actionSystem.ex.ComboBoxAction
        implements BackgroundUpdateAware, DumbAware {

    private static WeakRef<Balloon> currentBalloon;

    @NotNull
    @Override
    public JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {
        JPanel panel=new JPanel(new GridBagLayout());
        ComboBoxButton button = new ComboBoxButton(presentation);
        GridBagConstraints constraints = new GridBagConstraints(
                0, 0, 1, 1, 1, 1,
                GridBagConstraints.CENTER,
                GridBagConstraints.BOTH, JBUI.insets(3), 0, 0);
        panel.add(button, constraints);
        panel.setFocusable(false);
        return panel;
    }

    @Override
    protected JBPopup createActionPopup(DefaultActionGroup group, @NotNull DataContext context, @Nullable Runnable disposeCallback) {
        ListPopupImpl actionPopup = (ListPopupImpl) super.createActionPopup(group, context, disposeCallback);

        JList list = actionPopup.getList();
        list.addListSelectionListener(e -> showDescriptionPopup(context, e, actionPopup));

        return actionPopup;
    }

    private static void showDescriptionPopup(@NotNull DataContext dataContext, ListSelectionEvent e, ListPopupImpl actionPopup) {
        JList list = actionPopup.getList();

        if (e.getValueIsAdjusting()) return;
        if (!actionPopup.getContent().isShowing()) return;

        hideCurrentPopup();

        String content = getPopupContent(list);
        if (content == null) return;

        JPanel component = createPopupComponent(content);

        RelativePoint location = getPopupLocation(dataContext, component, list);
        if (location == null) return;

        Balloon balloon = createPopupBalloon(component, actionPopup);
        currentBalloon = WeakRef.of(balloon);

        balloon.show(location, Balloon.Position.atLeft);
    }

    private static @Nullable Rectangle getSelectionBounds(JList list) {
        int selectedIndex = list.getSelectedIndex();
        if (selectedIndex < 0) return null;

        return list.getCellBounds(selectedIndex, selectedIndex);
    }

    private static @Nullable String getPopupContent(JList list) {
        Object selectedValue = list.getSelectedValue();
        if (!(selectedValue instanceof PopupFactoryImpl.ActionItem actionItem)) return null;

        String description = actionItem.getDescription();
        if (description == null || description.isBlank()) return null;
        return description;
    }

    private static void hideCurrentPopup() {
        Balloon current = WeakRef.get(currentBalloon);
        if (current != null) {
            current.hide();
        }
    }

    private static Balloon createPopupBalloon(JPanel content, Disposable disposable) {
        BalloonBuilder builder = JBPopupFactory.getInstance().createBalloonBuilder(content);
        builder.setAnimationCycle(0);
        builder.setShowCallout(false);
        builder.setCornerRadius(JBUI.scale(8));
        builder.setLayer(Balloon.Layer.top);
        builder.setDisposable(disposable);
        builder.setFillColor(UIUtil.getTextFieldBackground());
        builder.setBorderColor(JBUI.CurrentTheme.Popup.borderColor(true));


        return builder.createBalloon();
    }

    private static JPanel createPopupComponent(String description) {
        String html = description.contains("<html>") ? description : "<html><body style='width: 200px; margin: 0;'>" +
                StringUtil.escapeXmlEntities(description) +
                "</body></html>";

        JBLabel label = new JBLabel(html);
        label.setOpaque(false);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(JBUI.Borders.empty(4, 4));
        content.add(label);
        return content;
    }

    private static RelativePoint getPopupLocation(DataContext dataContext, JPanel content, JList actionList) {
        Rectangle bounds = getSelectionBounds(actionList);
        if (bounds == null) return null;

        Component component = PlatformDataKeys.CONTEXT_COMPONENT.getData(dataContext);
        if (component == null) return null;

        Window window = SwingUtilities.getWindowAncestor(component);

        Dimension contentSize = content.getPreferredSize();
        int popupWidth = contentSize.width;
        int popupGap = JBUI.scale(16);

        Point boundsLocation = SwingUtilities.convertPoint(actionList, bounds.getLocation(), window);
        int left = boundsLocation.x - popupWidth - popupGap;

        int y = boundsLocation.y;
        int x = left < 0 ?
                boundsLocation.x + actionList.getWidth() + popupWidth / 2 + popupGap :
                boundsLocation.x - popupWidth / 2 - popupGap;

        return new RelativePoint(window, new Point(x, y));
    }

    @NotNull
    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return resolveActionUpdateThread();
    }

}
