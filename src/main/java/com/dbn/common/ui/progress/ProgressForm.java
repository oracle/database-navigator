/*
 * Copyright 2025 Oracle and/or its affiliates
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
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.UserInterface;
import com.intellij.openapi.Disposable;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import static com.dbn.common.util.Commons.nvl;

public class ProgressForm extends DBNFormBase {
    private JPanel mainPanel;
    private JProgressBar progressBar;
    private JLabel progressTextLabel;
    private JLabel progressText2Label;

    public ProgressForm(@Nullable Disposable parent) {
        super(parent);
        progressBar.setBorder(null);
        progressText2Label.setForeground(Colors.faded(UIUtil.getLabelForeground()));
        progressTextLabel.setText(" ");
        progressText2Label.setText(" ");
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public void setIndeterminate(boolean indeterminate) {
        progressBar.setIndeterminate(indeterminate);
    }

    public void setText(String text) {
        progressTextLabel.setText(nvl(text, " "));
    }

    public void setText2(String text2) {
        progressText2Label.setText(nvl(text2, " "));
    }

    public void setEnabled(boolean enabled) {
        progressBar.setEnabled(enabled);
    }

    public boolean matchesText(String text, String text2) {
        return UserInterface.matchesText(progressTextLabel, text) &&
                UserInterface.matchesText(progressText2Label, text2);
    }

    public void setMaximum(int maximum) {
        progressBar.setMaximum(maximum);
    }

    public void setValue(int value) {
        progressBar.setValue(value);
    }
}
