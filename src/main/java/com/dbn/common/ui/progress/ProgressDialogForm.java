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

import com.dbn.common.color.Colors;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.listener.KeyAdapter;
import com.dbn.common.util.Conditional;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.event.KeyEvent;
import java.util.Timer;
import java.util.TimerTask;

import static com.dbn.common.ui.util.UserInterface.matchesText;
import static com.dbn.common.util.Dialogs.resizeToFitContent;

public class ProgressDialogForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JProgressBar progressBar;
    private JLabel progressTextLabel;
    private JLabel progressText2Label;
    private JButton backgroundButton;
    private JButton cancelButton;

    private final ProgressDialogHandler handler;
    private final Timer progressUpdater = new Timer("DBN - Progress Dialog Updater", true);

    public ProgressDialogForm(@NotNull ProgressDialogHandler handler) {
        super(null, handler.getProject());
        this.handler = handler;

        headerPanel.setVisible(false); // TODO support environment colored headers
        progressBar.setIndeterminate(true);
        progressBar.setVisible(true);
        progressBar.setBorder(null);
        updateProgressLabels();

        KeyAdapter keyListener = createKeyListener();
        cancelButton.addKeyListener(keyListener);
        backgroundButton.addKeyListener(keyListener);

        cancelButton.addActionListener(e -> handler.cancel());
        backgroundButton.addActionListener(e -> handler.release());
        progressText2Label.setForeground(Colors.lafBrighter(Colors.getLabelForeground(), 10));
        progressUpdater.schedule(createProgressRefreshTask(), 100, 100);
    }

    private @NotNull TimerTask createProgressRefreshTask() {
        return new TimerTask() {
            @Override
            public void run() {
                updateProgressLabels();
            }
        };
    }

    private void updateProgressLabels() {
        ProgressIndicator progressIndicator = handler.getProgressIndicator();
        if (progressIndicator == null || !progressIndicator.isRunning()) {
            dispose();
            return;
        }

        String text = handler.getText();
        String text2 = handler.getText2();
        if (matchesText(progressTextLabel, text) &&
                matchesText(progressText2Label, text2)) return;

        Dispatch.run(mainPanel, () -> {
            progressTextLabel.setText(text);
            progressText2Label.setText(text2);
            resizeToFitContent(mainPanel);
        });
    }

    @NotNull
    private KeyAdapter createKeyListener() {
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

    @Override
    public void disposeInner() {
        progressUpdater.cancel();
        super.disposeInner();
    }
}
