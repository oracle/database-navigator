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

import com.dbn.common.util.Context;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Condition;

import java.util.List;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.util.Unsafe.cast;

/**
 * A builder class for creating and configuring action popups.
 *
 * This class provides an API to specify various properties and behaviors
 * for an action popup, which can display a list of actions in a popup menu.
 * The popup can be customized with titles, selection aids, visibility settings,
 * callbacks, and other configurations.
 *
 * @author Dan Cioca (Oracle)
 */
public class ActionPopupBuilder {
    private final ActionGroup actionGroup;
    private final DataContext dataContext;

    private String title;
    private JBPopupFactory.ActionSelectionAid selectionAid;

    private String actionPlace = ActionPlaces.POPUP;
    boolean titleVisible = true;
    private int maxRowCount = 10;

    private Condition<AnAction> preselectCondition;
    private Runnable disposeCallback;

    private ActionPopupBuilder(List<? extends AnAction> actions, DataContext dataContext) {
        this(new DefaultActionGroup(actions), dataContext);
    }

    private ActionPopupBuilder(ActionGroup actionGroup, DataContext dataContext) {
        this.actionGroup = actionGroup;
        this.dataContext = dataContext;
    }


    static ActionPopupBuilder create(List<? extends AnAction> actions, Object context) {
        return new ActionPopupBuilder(actions, Context.getDataContext(context));
    }

    static ActionPopupBuilder create(ActionGroup actionGroup, Object context) {
        return new ActionPopupBuilder(actionGroup, Context.getDataContext(context));
    }

    public ActionPopupBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    public ActionPopupBuilder withSpeedSearch() {
        return withSelectionAid(JBPopupFactory.ActionSelectionAid.SPEEDSEARCH);
    }

    public ActionPopupBuilder withSelectionAid(JBPopupFactory.ActionSelectionAid selectionAid) {
        this.selectionAid = selectionAid;
        return this;
    }

    public ActionPopupBuilder withTitleVisible(boolean titleVisible) {
        this.titleVisible = titleVisible;
        return this;
    }

    public ActionPopupBuilder withDisposeCallback(Runnable disposeCallback) {
        this.disposeCallback = disposeCallback;
        return this;
    }

    public ActionPopupBuilder withMaxRowCount(int maxRowCount) {
        this.maxRowCount = maxRowCount;
        return this;
    }

    public ActionPopupBuilder withPreselectCondition(Condition<? extends AnAction> preselectCondition) {
        this.preselectCondition = cast(preselectCondition);
        return this;
    }

    public ActionPopupBuilder withActionPlace(String actionPlace) {
        this.actionPlace = actionPlace;
        return this;
    }

    public ListPopup build() {
        String popupTitle = titleVisible ? title : null;
        ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                popupTitle,
                actionGroup,
                dataContext,
                selectionAid,
                true,
                disposeCallback,
                maxRowCount,
                preselectCondition,
                actionPlace);

        if (title != null && !titleVisible) {
            // set hidden accessibility title to action list
            setAccessibleName(popup, title);
        }
        return popup;
    }

    public void buildAndShow() {
        ListPopup popup = build();
        popup.showInBestPositionFor(dataContext);
    }

    public void buildAndShowCentered() {
        ListPopup popup = build();
        Project project = PlatformDataKeys.PROJECT.getData(dataContext);
        if (project == null)  {
            popup.showInBestPositionFor(dataContext);
        } else {
            popup.showCenteredInCurrentWindow(project);
        }
    }


}
