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

package com.dbn.common.ui.dialog;

import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.Presentable;
import com.dbn.common.ui.util.Listeners;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;

@Getter
@Setter
public class OptionsDialog<O extends Presentable> extends DBNDialog<OptionsDialogForm>{
    private final O[] options;
    private final String[] actionNames;
    private final String optionLabel;
    private Action[] actions;
    private O selectedOption;

    private final Listeners<OptionDialogActionListener<O>> listeners = Listeners.create(this);

    protected OptionsDialog(Project project, String dialogTitle, String optionLabel, O[] options, O selectedOption, String[] actionNames) {
        super(project, dialogTitle, false);
        this.optionLabel = optionLabel;
        this.options = options;
        this.actionNames = actionNames;
        this.selectedOption = selectedOption;

        setDefaultSize(600, 300);
        setActionsEnabled(selectedOption != null);
        //setResizable(false);
        init();
    }

    @Override
    protected String getDimensionServiceKey() {
        //return super.getDimensionServiceKey() + "." + options[0].getClass().getSimpleName();
        return null;
    }

    @Override
    @NotNull
    protected final Action[] createActions() {
        Action[] actions = new Action[this.actionNames.length + 1];
        String[] strings = this.actionNames;
        for (int i = 0; i < strings.length; i++) {
            String action = strings[i];
            actions[i] = createAction(action, i);
        }
        actions[this.actionNames.length] = getCancelAction();
        this.actions = actions;
        return actions;
    }

    @NotNull
    private Action createAction(String name, int index) {
        AbstractAction action = new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent e) {
                listeners.notify(l -> l.actionPerformed(index, selectedOption));
                close(index);
            }
        };
        return action;
    }

    public void setActionsEnabled(boolean enabled) {
        Action[] actions = this.actions;
        if (actions == null) return;
        for (Action action : actions) {
            if (action == getCancelAction()) continue;
            action.setEnabled(enabled);
        }
    }

    public void addActionListener(OptionDialogActionListener<O> actionListener) {
        listeners.add(actionListener);
    }

    @NotNull
    @Override
    protected OptionsDialogForm<O> createForm() {
        return new OptionsDialogForm<>(this);
    }

    public static <O extends Presentable> void open(
            Project project,
            String dialogTitle,
            String optionLabel,
            O[] options,
            O selectedOption,
            String[] actions,
            OptionDialogActionListener<O> actionListener) {
        Dispatch.run(() -> {
            OptionsDialog<O> optionsDialog = new OptionsDialog<>(project, dialogTitle, optionLabel, options, selectedOption, actions);
            optionsDialog.addActionListener(actionListener);
            optionsDialog.show();
        });
    }

}

