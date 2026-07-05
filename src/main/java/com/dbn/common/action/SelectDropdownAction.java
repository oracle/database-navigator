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
import com.dbn.common.ui.Presentable;
import com.dbn.common.util.Actions;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.util.List;

import static com.dbn.nls.NlsResources.txt;

@BackgroundUpdate
public abstract class SelectDropdownAction<T extends Presentable> extends ComboBoxAction implements DumbAware {
    private transient WeakRef<T> lastSelection;

    protected SelectDropdownAction(@Nullable String text) {
        super(text);
    }

    @Nullable
    protected List<T> getObjects(AnActionEvent e) {
        return getObjects(e.getDataContext());
    }

    @Nullable
    protected abstract List<T> getObjects(DataContext dataContext);

    protected abstract T getSelectedObject(AnActionEvent e);

    protected abstract void setSelectedObject(AnActionEvent e, T object);

    protected String getEmptySelectionText(AnActionEvent e) {
        return "";
    }

    protected String getDescription(AnActionEvent e) {
        return null;
    }

    protected String getDescription(T element) {
        return null;
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
        if (objects == null) return actionGroup;

        for (T object : objects) {
            actionGroup.add(new ElementSelectAction(object));
        }
        return actionGroup;
    }

    @Override
    protected Condition<AnAction> getPreselectCondition() {
        return a -> {
            if (a instanceof SelectDropdownAction.ElementSelectAction selectAction) {
                return selectAction.element == WeakRef.get(lastSelection);
            }
            return false;
        };
    }

    @Override
    public final void update(@NotNull AnActionEvent e) {
        T object = getSelectedObject(e);
        lastSelection = WeakRef.of(object);

        Presentation presentation = e.getPresentation();
        presentation.setVisible(isVisible(e));
        presentation.setEnabled(isEnabled(e));

        if (object == null) {
            if (isLoading(e)) {
                presentation.setText(txt("app.shared.action.Loading"));
                presentation.setEnabled(false);
            } else {
                String emptySelectionText = getEmptySelectionText(e);
                presentation.setText(emptySelectionText);
            }
            presentation.setIcon(null);
        } else {
            presentation.setText(Actions.adjustActionName(object.getName()));
            presentation.setIcon(object.getIcon());
        }

        presentation.setDescription(getDescription(e));
    }

    @BackgroundUpdate
    private class ElementSelectAction extends ProjectAction {
        private final T element;
        protected ElementSelectAction(T element) {
            this.element = element;
        }

        @Override
        protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
            setSelectedObject(e, element);
        }

        @Override
        protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
            Presentation presentation = e.getPresentation();
            String text = Actions.adjustActionName(element.getName());


            presentation.setText(text);
            presentation.setIcon(element.getIcon());
            presentation.setDescription(getDescription(element));
        }
    }
}
