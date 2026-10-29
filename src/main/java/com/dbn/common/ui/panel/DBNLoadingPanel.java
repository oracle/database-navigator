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

package com.dbn.common.ui.panel;

import com.dbn.common.action.BasicAction;
import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Actions;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.util.function.Supplier;

import static com.dbn.nls.NlsResources.txt;

public class DBNLoadingPanel extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel loadingIconPanel;
    private JLabel loadingLabel;
    private JPanel actionsPanel;

    public DBNLoadingPanel(@Nullable Disposable parent, @Nls String loadingText) {
        super(parent);
        actionsPanel.setVisible(false);
        loadingLabel.setText(loadingText);
        loadingLabel.setForeground(Colors.getContextHelpForeground());
        loadingIconPanel.add(new AsyncProcessIcon(txt("app.shared.action.Loading")));
    }

    public static DBNLoadingPanel newLoadingPanel(@Nullable Disposable parent, @Nls String loadingText) {
        return new DBNLoadingPanel(parent, loadingText);
    }

    public void installOn(JPanel panel, boolean visible) {
        panel.add(getComponent(), BorderLayout.WEST);
        panel.setVisible(visible);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public DBNLoadingPanel withLoadingText(@Nls String loadingText) {
        loadingLabel.setText(loadingText);
        return  this;
    }

    public DBNLoadingPanel withCancelAction(Runnable runnable, Supplier<Boolean> enabled) {
        CancelLoadingAction cancelAction = new CancelLoadingAction(runnable, enabled);
        ActionToolbar loadingActionToolbar = Actions.createActionToolbar(actionsPanel, true, cancelAction);
        actionsPanel.add(loadingActionToolbar.getComponent());
        actionsPanel.setVisible(true);
        return this;
    }

    private static class CancelLoadingAction extends BasicAction {
        private final Runnable runnable;
        private final Supplier<Boolean> enabled;

        public CancelLoadingAction(Runnable runnable, Supplier<Boolean> enabled) {
            super(txt("app.shared.action.Cancel"), null, Icons.DATA_EDITOR_STOP_LOADING);
            this.runnable = runnable;
            this.enabled = enabled;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            runnable.run();
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            presentation.setEnabled(enabled.get());
        }
    }
}
