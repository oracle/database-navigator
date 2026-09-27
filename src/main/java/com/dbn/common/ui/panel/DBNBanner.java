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

import com.dbn.common.color.Colors;
import com.dbn.common.message.MessageType;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import static com.dbn.common.ui.link.Hyperlinks.onHyperlinkAccess;

/**
 * A DBN-compatible inline banner based on IntelliJ's themed banner colors.
 *
 * <p>This intentionally does not extend {@code com.intellij.ui.InlineBanner}.
 * That component is not available in all platform versions supported by DBN,
 * while the banner colors and layout primitives are.</p>
 */
public class DBNBanner extends JPanel {
    private static final int MINIMUM_WIDTH = 256;
    private static final int CORNER_RADIUS = 16;

    private final JPanel iconPanel = new JPanel(new BorderLayout());
    private final JLabel iconLabel = new JLabel();
    private final JPanel centerPanel = new JPanel(new BorderLayout(0, JBUI.scale(8)));
    private final JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, JBUI.scale(16), 0));
    private final JEditorPane message = new JEditorPane();

    private MessageType messageType;

    public DBNBanner(@NotNull MessageType messageType) {
        this(null, messageType);
    }

    public DBNBanner(@Nullable @Nls String message, @NotNull MessageType messageType) {
        super(new BorderLayout(JBUI.scale(8), JBUI.scale(8)));
        this.messageType = messageType;

        setOpaque(false);
        setBorder(JBUI.Borders.empty(12));

        iconPanel.setOpaque(false);
        iconPanel.add(iconLabel, BorderLayout.NORTH);
        add(iconPanel, BorderLayout.WEST);

        centerPanel.setOpaque(false);
        actionsPanel.setOpaque(false);
        actionsPanel.setVisible(false);

        configureMessage();
        centerPanel.add(this.message, BorderLayout.CENTER);
        centerPanel.add(actionsPanel, BorderLayout.SOUTH);
        add(centerPanel, BorderLayout.CENTER);

        updateStyle();
        setMessage(message);
    }

    public DBNBanner(@NotNull MessageType messageType, @Nullable @Nls String message) {
        this(message, messageType);
    }

    private void configureMessage() {
        message.setEditable(false);
        message.setOpaque(false);
        message.setBorder(JBUI.Borders.empty());
        message.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        message.setContentType("text/html");
        message.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE);
    }

    public DBNBanner setMessage(@Nullable @Nls String message) {
        this.message.setText(message == null ? "" : message);
        if (this.message.getCaret() != null) {
            this.message.setCaretPosition(0);
        }
        return this;
    }

    public DBNBanner setMessageType(@NotNull MessageType messageType) {
        this.messageType = messageType;
        updateStyle();
        revalidate();
        repaint();
        return this;
    }

    public DBNBanner setIcon(@Nullable Icon icon) {
        iconLabel.setIcon(icon);
        iconPanel.setVisible(icon != null);
        revalidate();
        repaint();
        return this;
    }

    public DBNBanner addAction(@NotNull @Nls String text, @NotNull Runnable action) {
        HyperlinkLabel actionLink = new HyperlinkLabel();
        actionLink.setHyperlinkText(text);
        onHyperlinkAccess(actionLink, event -> action.run());
        actionsPanel.add(actionLink);
        actionsPanel.setVisible(true);
        revalidate();
        repaint();
        return this;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, Math.max(width, JBUI.scale(MINIMUM_WIDTH)), height);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth() - 1;
            int height = getHeight() - 1;
            if (width <= 0 || height <= 0) return;

            int arc = JBUI.scale(CORNER_RADIUS);
            g.setColor(getBackground());
            g.fillRoundRect(0, 0, width, height, arc, arc);
            g.setColor(getBorderColor(messageType));
            g.drawRoundRect(0, 0, width, height, arc, arc);
        } finally {
            g.dispose();
        }
    }

    private void updateStyle() {
        setBackground(getBackground(messageType));
        message.setBackground(getBackground(messageType));
        message.setForeground(Colors.Banner.FOREGROUND);
        iconLabel.setForeground(Colors.Banner.FOREGROUND);
        setIcon(messageType.getTitleIcon());
    }

    private static Color getBackground(@NotNull MessageType messageType) {
        return switch (messageType) {
            case INFO -> Colors.Banner.INFO_BACKGROUND_COLOR;
            case SUCCESS -> Colors.Banner.SUCCESS_BACKGROUND_COLOR;
            case WARNING -> Colors.Banner.WARNING_BACKGROUND_COLOR;
            case ERROR -> Colors.Banner.ERROR_BACKGROUND_COLOR;
            default -> Colors.getPanelBackground();
        };
    }

    private static Color getBorderColor(@NotNull MessageType messageType) {
        return switch (messageType) {
            case INFO -> Colors.Banner.INFO_BORDER_COLOR;
            case SUCCESS -> Colors.Banner.SUCCESS_BORDER_COLOR;
            case WARNING -> Colors.Banner.WARNING_BORDER_COLOR;
            case ERROR -> Colors.Banner.ERROR_BORDER_COLOR;
            default -> Colors.getOutlineColor();
        };
    }
}
