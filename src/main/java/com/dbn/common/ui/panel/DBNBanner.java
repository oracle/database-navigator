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
import com.dbn.common.icon.Icons;
import com.dbn.common.message.MessageType;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.ui.text.HiddenCaret;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.util.ui.HTMLEditorKitBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import static com.dbn.common.ui.link.Hyperlinks.onHyperlinkAccess;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.nls.NlsResources.txt;

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

    private JPanel mainPanel;
    private JPanel iconPanel;
    private JTextPane messageTextPane;
    private JTextPane detailsTextPane;
    private DBNHyperlinkLabel detailsToggle;
    private JPanel actionsPanel;

    private MessageType messageType;

    public DBNBanner(@NotNull MessageType messageType) {
        this(null, messageType);
    }

    public DBNBanner(@Nullable @Nls String message, @NotNull MessageType messageType) {
        super(new BorderLayout());
        this.messageType = messageType;

        setOpaque(false);
        add(mainPanel, BorderLayout.CENTER);

        configureTextPane(messageTextPane);
        configureTextPane(detailsTextPane);
        detailsToggle.setHyperlinkText(txt("app.shared.link.ShowMore"));
        onHyperlinkAccess(detailsToggle, event -> showDetails());

        detailsTextPane.setVisible(false);
        detailsToggle.setVisible(false);
        actionsPanel.setVisible(false);

        updateStyle();
        setMessage(message);
    }

    public DBNBanner(@NotNull MessageType messageType, @Nullable @Nls String message) {
        this(message, messageType);
    }

    private static void configureTextPane(JTextPane textPane) {
        textPane.setCaret(new HiddenCaret());
        textPane.putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        textPane.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE);
    }

    public DBNBanner setMessage(@Nullable @Nls String message) {
        return setMessage(message, null);
    }

    public DBNBanner setMessage(
            @Nullable @Nls String message,
            @Nullable @Nls String details) {
        setText(messageTextPane, message);
        setDetails(details);
        return this;
    }

    public DBNBanner setDetails(@Nullable @Nls String details) {
        setText(detailsTextPane, details);
        detailsTextPane.setVisible(false);
        detailsToggle.setVisible(isNotEmpty(details));
        revalidate();
        repaint();
        return this;
    }

    private static void setText(JTextPane textPane, @Nullable @Nls String text) {
        String value = text == null ? "" : text;
        if (isHtml(value)) {
            textPane.setEditorKit(new HTMLEditorKitBuilder().withWordWrapViewFactory().build());
        } else {
            textPane.setContentType("text/plain");
        }
        textPane.setText(value);
        textPane.setCaretPosition(0);
    }

    private static boolean isHtml(String text) {
        return text.trim().startsWith("<html>");
    }

    private void showDetails() {
        detailsTextPane.setVisible(true);
        detailsTextPane.setCaretPosition(0);
        detailsToggle.setVisible(false);
        revalidate();
        repaint();
    }

    public DBNBanner setMessageType(@NotNull MessageType messageType) {
        this.messageType = messageType;
        updateStyle();
        revalidate();
        repaint();
        return this;
    }

    public DBNBanner setIcon(@Nullable Icon icon) {
        iconPanel.removeAll();
        if (icon == null) {
            iconPanel.setVisible(false);
        } else {
            JLabel iconLabel = new JLabel(icon);
            iconPanel.add(iconLabel, BorderLayout.NORTH);
            iconPanel.setVisible(true);
        }
        revalidate();
        repaint();
        return this;
    }

    public DBNBanner addAction(@NotNull @Nls String text, @NotNull Runnable action) {
        DBNHyperlinkLabel actionLink = new DBNHyperlinkLabel();
        actionLink.setBorder(JBUI.Borders.empty());
        actionLink.setHyperlinkText(text);
        onHyperlinkAccess(actionLink, event -> action.run());

        if (actionsPanel.getComponentCount() > 0) {
            actionsPanel.add(Box.createHorizontalStrut(JBUI.scale(8)));
        }
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
        messageTextPane.setBackground(getBackground(messageType));
        messageTextPane.setForeground(Colors.Banner.FOREGROUND);
        detailsTextPane.setBackground(getBackground(messageType));
        detailsTextPane.setForeground(Colors.Banner.FOREGROUND);
        setIcon(getIcon(messageType));
    }

    private static Icon getIcon(@NotNull MessageType messageType) {
        return switch (messageType) {
            case SUCCESS -> Icons.COMMON_SUCCESS;
            case WARNING -> Icons.COMMON_WARNING;
            case INFO -> Icons.COMMON_INFO;
            case ERROR -> Icons.COMMON_ERROR;
            default -> messageType.getTitleIcon();
        };
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
