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
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.TimeUtil;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class DialogWithTimeoutForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel contentPanel;
    private JLabel timeLeftLabel;

    public DialogWithTimeoutForm(DBNDialog<?> parent, int secondsLeft) {
        super(parent);
        contentPanel.setBorder(Borders.BOTTOM_LINE_BORDER);
        updateTimeLeft(secondsLeft);
    }

    public void setContentComponent(JComponent contentComponent) {
        contentPanel.add(contentComponent, BorderLayout.CENTER);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public void updateTimeLeft(int secondsLeft) {
        Dispatch.run(() -> {
            int minutes = 0;
            int seconds = secondsLeft;
            if (secondsLeft > 60) {
                minutes = TimeUtil.getMinutes(secondsLeft);
                seconds = secondsLeft - TimeUtil.getSeconds(minutes);
            }

            if (minutes == 0) {
                timeLeftLabel.setText(seconds + " seconds");
                timeLeftLabel.setForeground(JBColor.RED);
            } else {
                timeLeftLabel.setText(minutes +":" + (seconds < 10 ? "0" :"") + seconds + " minutes");
            }
        });
    }
}
