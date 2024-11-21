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

package com.dbn.data.grid.ui.table.basic;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.MathResult;
import com.intellij.ide.IdeTooltipManager;
import com.intellij.openapi.project.Project;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;

public class MathPanel extends DBNFormBase {
    private JLabel sumLabel;
    private JLabel countLabel;
    private JLabel averageLabel;
    private JPanel mainPanel;

    public MathPanel(Project project, MathResult result) {
        super(null, project);
        sumLabel.setText(result.getSum().toPlainString());
        countLabel.setText(result.getCount().toPlainString());
        averageLabel.setText(result.getAverage().toPlainString());
        Color background = IdeTooltipManager.getInstance().getTextBackground(true);
        mainPanel.setBackground(background);
    }


    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
