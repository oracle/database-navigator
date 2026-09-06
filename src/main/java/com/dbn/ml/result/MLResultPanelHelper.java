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

package com.dbn.ml.result;

import com.dbn.common.ui.info.DBNInfoLabel;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

import static com.dbn.common.text.TextContent.html;
import static com.dbn.common.text.TextResources.getLocalizable;

/**
 * Static helpers for building repeating ML result panel patterns.
 */
public final class MLResultPanelHelper {

    private MLResultPanelHelper() {}

    /** Compound border used consistently across all ML result sections. */
    public static Border sectionBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.border(), 1),
                JBUI.Borders.empty(12));
    }

    /**
     * Initialises a section panel: BorderLayout, compound border, and a bold title label
     * pinned to NORTH.
     */
    public static void initSection(JPanel panel, @Nls String title) {
        initSection(panel, title, null);
    }

    /**
     * Same as {@link #initSection(JPanel, String)}, additionally showing an info icon next to the
     * title which reveals the given html content on hover or click.
     *
     * @param infoResourceName name of the html template, relative to {@code context}
     */
    public static void initSection(JPanel panel, @Nls String title, @Nullable @NonNls String infoResourceName) {
        panel.setLayout(new BorderLayout(8, 8));
        panel.setBorder(sectionBorder());

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));

        if (infoResourceName == null) {
            panel.add(titleLabel, BorderLayout.NORTH);
            return;
        }

        DBNInfoLabel infoLabel = new DBNInfoLabel();
        infoLabel.setContent(html(getLocalizable(MLResultPanelHelper.class, infoResourceName)));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        titlePanel.setOpaque(false);
        titlePanel.add(titleLabel);
        titlePanel.add(infoLabel);
        panel.add(titlePanel, BorderLayout.NORTH);
    }
}
