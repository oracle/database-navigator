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

package com.dbn.common.ui.messages;

import com.dbn.common.compatibility.Workaround;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Titles;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts.DialogMessage;
import com.intellij.openapi.util.NlsContexts.DialogTitle;
import com.intellij.ui.AppIcon;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.util.List;

public class DBNMessageDialog extends DBNDialog<DBNMessageForm> {
    private final Icon icon;
    private final String title;
    private final String message;
    private final String[] options;
    private final int defaultOptionIndex;

    private Action[] actions;

    public DBNMessageDialog(
            @Nullable Project project,
            @Nullable Icon icon,
            @NotNull @DialogTitle String title,
            @NotNull @DialogMessage String message,
            @NotNull String[] options,
            int defaultOptionIndex,
            @Nullable DoNotAskOption rememberOption) {
        super(project, title, false);
        this.icon = icon;
        this.title = Titles.signed(title);
        this.message = message;

        this.options = options;
        this.defaultOptionIndex = defaultOptionIndex;

        this.setDoNotAskOption(rememberOption);

        initDecoration();
        initActions();
        init();

        initRememberOption();
        initAccessibility();
    }

    private void initRememberOption() {
        DBNMessageForm form = getForm();
        form.initRememberOption(myCheckBoxDoNotShowDialog);
    }

    @Override
    protected @Nullable JComponent createDoNotAskCheckbox() {
        return null; // custom layout (do not create default)
    }

    private void initActions() {
        actions = new Action[options.length];
        for (int i = 0; i < options.length; i++) {
            actions[i] = createAction(i);
        }
        if (defaultOptionIndex > -1 && defaultOptionIndex < actions.length) {
            Action defaultAction = actions[defaultOptionIndex];
            makeDefaultAction(defaultAction);
            makeFocusAction(defaultAction);
        }
    }

    private void initAccessibility() {
        //...
    }

    private void initDecoration() {
        Dialogs.undecorate(this, true);
    }

    @Override
    public void show() {
        AppIcon.getInstance().requestAttention(getProject(), true);
        super.show();
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return actions;
    }

    //@Override
    @Workaround
    protected void sortActionsOnMac(@NotNull List<Action> actions) {
        // TODO proper action sequence support needed in "Messages" utility
    }

    private @NotNull AbstractAction createAction(int exitCode) {
        String actionName = UIUtil.replaceMnemonicAmpersand(options[exitCode]);
        return new AbstractAction(actionName) {
            @Override
            public void actionPerformed(ActionEvent e) {
                close(exitCode);
            }

            @Override
            public String toString() {
                return actionName;
            }
        };
    }

    @Override
    protected String getDimensionServiceKey() {
        return null; // always reset location (show in best position for context)
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return null; // overwrite first focusable component to force focus on buttons
    }

    @Override
    protected @NotNull DBNMessageForm createForm() {
        return new DBNMessageForm(this, icon, title, message);
    }
}
