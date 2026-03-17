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

package com.dbn.object.action;

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.ComboBoxAction;
import com.dbn.common.util.Actions;
import com.dbn.object.common.DBObject;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.util.List;

@BackgroundUpdate
public abstract class ObjectSelectDropdownAction<T extends DBObject> extends ComboBoxAction implements DumbAware {

    protected List<T> getObjects(AnActionEvent e) {
        return getObjects(e.getDataContext());
    }

    protected abstract List<T> getObjects(DataContext dataContext);

    protected abstract T getSelectedObject(AnActionEvent e);

    protected abstract void setSelectedObject(AnActionEvent e, T object);

    protected String getEmptySelectionText(AnActionEvent e) {
        return "";
    }

    protected boolean isVisible(AnActionEvent e) {
        return true;
    }

    protected boolean isEnabled(AnActionEvent e) {
        return true;
    }

    protected boolean isLoading(AnActionEvent e) {
        return false;
    }

    @NotNull
    @Override
    protected final DefaultActionGroup createPopupActionGroup(@NotNull JComponent button, @NotNull DataContext dataContext) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        List<T> objects = getObjects(dataContext);
        for (T object : objects) {
            actionGroup.add(new ObjectSelectAction(object));
        }
        return actionGroup;
    }

    @Override
    public final void update(@NotNull AnActionEvent e) {
        T object = getSelectedObject(e);
        Presentation presentation = e.getPresentation();
        presentation.setVisible(isVisible(e));
        presentation.setEnabled(isEnabled(e));

        if (object == null) {
            if (isLoading(e)) {
                presentation.setText("Loading...");
                presentation.setIcon(null);
                presentation.setEnabled(false);
            } else {
                String emptySelectionText = getEmptySelectionText(e);
                presentation.setText(emptySelectionText);
                presentation.setIcon(null);
            }
        } else {
            presentation.setText(Actions.adjustActionName(object.getName()));
            presentation.setIcon(object.getIcon());
        }
    }

    @BackgroundUpdate
    private class ObjectSelectAction extends AnObjectAction<T> {
        protected ObjectSelectAction(T object) {
            super(object);
        }

        @Override
        protected void actionPerformed(
                @NotNull AnActionEvent e,
                @NotNull Project project,
                @NotNull T object) {

            setSelectedObject(e, object);
        }
    }
}
