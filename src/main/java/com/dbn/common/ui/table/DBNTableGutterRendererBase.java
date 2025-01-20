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

package com.dbn.common.ui.table;

import com.dbn.common.color.Colors;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.latent.Latent;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Fonts;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.util.HashMap;
import java.util.Map;

public abstract class DBNTableGutterRendererBase implements DBNTableGutterRenderer{
    protected JLabel textLabel;
    protected JLabel iconLabel;
    protected JPanel mainPanel;

    private final Latent<Map<Integer, Integer>> indexWidth = Latent.mutable(
            () -> textLabel.getFont(),
            () -> new HashMap<>());

    public DBNTableGutterRendererBase() {
        textLabel.setText("");
        iconLabel.setText("");
        textLabel.setFont(Fonts.editor(-2));
        textLabel.setForeground(Colors.getTableGutterForeground());
        mainPanel.setBackground(Colors.getTableGutterBackground());
        mainPanel.setPreferredSize(new Dimension(40, -1));
        iconLabel.setBorder(Borders.insetBorder(4));

        mainPanel.setBorder(Borders.tableBorder(0, 0, 0, 1));
    }

    @Override
    public final Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        adjustListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        textLabel.setText(Integer.toString(index + 1));
        int textWidth = computeLabelWidth(list.getModel().getSize());
        int iconWidth = iconLabel.getIcon() == null ? 0 : 16;
        //iconLabel.setVisible(iconLabel.getIcon() == null);

        int preferredWidth = textWidth + iconWidth + 16;

        Dimension preferredSize = mainPanel.getPreferredSize();
        if (preferredSize.getWidth() != preferredWidth) {
            Dimension dimension = new Dimension(preferredWidth, -1);
            mainPanel.setPreferredSize(dimension);
            Dispatch.run(() -> resize(list, preferredWidth));
        }
        return mainPanel;
    }

    private void resize(JList list, int preferredWidth) {
        Failsafe.nd(list);
        list.setPreferredSize(new Dimension(preferredWidth, (int) list.getPreferredSize().getHeight()));
    }

    private int computeLabelWidth(int count) {
        return indexWidth.get().computeIfAbsent(count, c -> {
            int digits = (int) Math.log10(c) + 1;
            String text = StringUtils.leftPad("", digits, "0");
            Font font = textLabel.getFont();
            FontRenderContext fontRenderContext = textLabel.getFontMetrics(font).getFontRenderContext();
            return (int) font.getStringBounds(text, fontRenderContext).getWidth();
        });
    }

    protected abstract void adjustListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus);
}
