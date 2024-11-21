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

package com.dbn.common.ui.misc;

import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Fonts;
import com.dbn.data.grid.options.DataGridGeneralSettings;
import com.dbn.data.grid.options.DataGridSettings;
import com.dbn.data.grid.ui.table.basic.BasicTable;
import com.intellij.ide.IdeTooltip;
import com.intellij.ide.IdeTooltipManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.Alarm;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseWheelEvent;

public class DBNTableScrollPane extends DBNScrollPane{
    private final Alarm resizeAlarm = new Alarm();
    private transient Font font;

    @Override
    public void setViewportView(Component view) {
        super.setViewportView(view);

        JPanel panel = new JPanel();
        panel.setBorder(Borders.tableBorder( 0, 0, 1, 1));
        setCorner(UPPER_LEFT_CORNER, panel);

    }

    protected void processMouseWheelEvent(MouseWheelEvent e) {
        if (!resizeContent(e)) super.processMouseWheelEvent(e);
    }

    private boolean resizeContent(MouseWheelEvent e) {
        if (!e.isControlDown()) return false;

        Component view = getViewComponent();
        if (!(view instanceof BasicTable)) return false;

        BasicTable resultTable = (BasicTable) view;
        Project project = resultTable.getProject();
        DataGridSettings dataGridSettings = DataGridSettings.getInstance(project);
        DataGridGeneralSettings generalSettings = dataGridSettings.getGeneralSettings();
        if (!generalSettings.isZoomingEnabled()) return false;

        if (font == null) font = resultTable.getFont();

        float size = font.getSize() - e.getWheelRotation();
        if (size > 7 && size < 20) {
            font = Fonts.deriveFont(font, size);
            float defaultSize = Fonts.getLabelFont().getSize();
            int percentage = (int) (size / defaultSize * 100);

            Dispatch.alarmRequest(resizeAlarm, 10, true, () -> {
                resultTable.setFont(font);
                IdeTooltip tooltip = new IdeTooltip(this, e.getPoint(), new JLabel(percentage + "%"));
                tooltip.setFont(Fonts.deriveFont(Fonts.REGULAR, (float) 16));
                IdeTooltipManager.getInstance().show(tooltip, true);
            });
            return true;
        }
        return false;
    }
}
