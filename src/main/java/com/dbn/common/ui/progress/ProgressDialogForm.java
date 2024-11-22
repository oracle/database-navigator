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

package com.dbn.common.ui.progress;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.listener.KeyAdapter;
import com.dbn.common.util.Conditional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.event.KeyEvent;

public class ProgressDialogForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JProgressBar progressBar;
    private JLabel progressTitleLabel;
    private JButton backgroundButton;
    private JButton cancelButton;

    public ProgressDialogForm(@NotNull ProgressDialogHandler handler) {
        super(null, handler.getProject());

        headerPanel.setVisible(false); // TODO support environment colored headers
        progressBar.setIndeterminate(true);
        progressBar.setVisible(true);
        progressBar.setBorder(null);
        progressTitleLabel.setText(handler.getText());

        KeyAdapter keyListener = createKeyListener(handler);
        cancelButton.addKeyListener(keyListener);
        backgroundButton.addKeyListener(keyListener);

        cancelButton.addActionListener(e -> handler.cancel());
        backgroundButton.addActionListener(e -> handler.release());
    }

    @NotNull
    private static KeyAdapter createKeyListener(@NotNull ProgressDialogHandler handler) {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                Conditional.when(e.getKeyCode() == KeyEvent.VK_ESCAPE, () -> handler.cancel());
            }
        };
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return backgroundButton;
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
