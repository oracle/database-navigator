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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import org.jetbrains.annotations.NotNull;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.InputEvent;

import static com.dbn.common.ui.util.Popups.popupBuilder;
import static java.util.Arrays.stream;

public abstract class ProjectPopupAction extends ProjectAction {
    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        AnAction[] children = getChildren(e);
        stream(children).forEach(a -> actionGroup.add(a));

        InputEvent inputEvent = e.getInputEvent();
        if (inputEvent == null) return;

        Component component = (Component) inputEvent.getSource();
        if (!component.isShowing()) return;

        String title = getTemplatePresentation().getText();
        ListPopup popup = popupBuilder(actionGroup, e).
                withTitle(title).
                withTitleVisible(false).
                withSpeedSearch().
                withMaxRowCount(10).
                build();

        showBelowComponent(popup, component);
    }

    private static void showBelowComponent(ListPopup popup, Component component) {
        Point locationOnScreen = component.getLocationOnScreen();
        Point location = new Point(
                (int) (locationOnScreen.getX() + 10),
                (int) locationOnScreen.getY() + component.getHeight());
        popup.showInScreenCoordinates(component, location);
    }


    public abstract AnAction[] getChildren(AnActionEvent e);
}
