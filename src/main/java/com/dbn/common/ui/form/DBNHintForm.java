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

package com.dbn.common.ui.form;

import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.message.MessageType;
import com.dbn.common.text.MimeType;
import com.dbn.common.text.TextContent;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.util.LookAndFeel;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.RoundedLineBorder;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import static com.dbn.common.text.HtmlContents.initFonts;
import static com.dbn.common.ui.util.ClientProperty.RESIZING;
import static com.dbn.common.ui.util.UserInterface.adjustDimension;

public class DBNHintForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel contentPanel;
    private JPanel iconPanel;
    private JLabel iconLabel;
    private JTextPane hintTextPane;
    private HyperlinkLabel actionLink;

    private final boolean boxed;
    private boolean highlighted;
    private TextContent content;

    public DBNHintForm(DBNForm parent, @Nullable TextContent hintContent, MessageType messageType, boolean boxed) {
        this(parent, hintContent, messageType, boxed, null, null);
    }

    public DBNHintForm(DBNForm parent, @Nullable TextContent hintContent, MessageType messageType, boolean boxed, String actionText, Runnable action) {
        super(parent);
        this.boxed = boxed;
        iconLabel.setText("");
        setMessageType(messageType);
        setHintContent(hintContent);

        updateComponentColors();
        if (boxed) {
            mainPanel.setBorder(new RoundedLineBorder(Colors.getOutlineColor(), 2));
            //mainPanel.setBorder(new RoundedLineBorder(UIManager.getColor("TextField.borderColor"), 3));
            //mainPanel.setBorder(UIUtil.getTextFieldBorder());
        } else {
            GridLayoutManager gridLayoutManager = (GridLayoutManager) contentPanel.getLayout();
            gridLayoutManager.setMargin(JBUI.emptyInsets());
        }

        if (actionText != null) {
            actionLink.setVisible(true);
            actionLink.setHyperlinkText(actionText);
            actionLink.addHyperlinkListener(e -> action.run());
        } else {
            actionLink.setVisible(false);
        }

        // workaround to force the text pane to resize to fit the content (alternative suggestions welcome)
        hintTextPane.setPreferredSize(new Dimension(-1, 500));
        hintTextPane.addPropertyChangeListener(e -> {
            if ("font".equals(e.getPropertyName())) {
                // appearance: accessibility zoom changes
                updateHintContent();
            }
        });

        mainPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeComponent();
            }
        });
    }

    private void updateComponentColors() {
        Color background = getBackground();
        Color foreground = getForeground();
        contentPanel.setBackground(background);
        contentPanel.setForeground(foreground);
        contentPanel.setForeground(foreground);
    }

    @Override
    protected void lookAndFeelChanged() {
        updateComponentColors();
    }

    @SneakyThrows
    private void resizeComponent() {
        Dispatch.run(mainPanel, () -> {
            if (RESIZING.is(mainPanel)) return;

            try {
                RESIZING.set(mainPanel, true);
                doResizeComponent();
            } finally {
                RESIZING.set(mainPanel, false);
            }
        });
    }

    private void doResizeComponent() {
        Dimension preferredSize = hintTextPane.getPreferredSize();
        hintTextPane.revalidate();

        Dimension contentPreferredSize = contentPanel.getPreferredSize();
        mainPanel.setPreferredSize(adjustDimension(contentPreferredSize, 0, 10));
        mainPanel.revalidate();

        Dimension contentSize = adjustDimension(preferredSize, 4, 4);
        hintTextPane.setPreferredSize(contentSize);
        hintTextPane.revalidate();
    }

    @NotNull
    private Color getBackground() {
        if (highlighted) return Colors.getTextFieldBackground();
        if (boxed) {
            return LookAndFeel.isDarkMode() ?
                    Colors.lafDarker(Colors.getPanelBackground(), 1) :
                    Colors.lafBrighter(Colors.getPanelBackground(), 1);
        }

        return Colors.getPanelBackground();
    }

    private Color getForeground() {
        return boxed ? Colors.lafBrighter(Colors.getLabelForeground(), 1) : Colors.HINT_COLOR;
    }

    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
        updateComponentColors();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public void setHintContent(@Nullable TextContent content) {
        this.content = content;
        updateHintContent();
    }

    private void updateHintContent() {
        if (content == null) {
            initEmptyContent();
        } else if (content.isHtml()) {
            initHtmlContent();
        } else {
            initPlainContent();
        }

        resizeComponent();
    }

    private void initPlainContent() {
        hintTextPane.setContentType(content.getTypeId());
        hintTextPane.setText(content.getText());
    }

    private void initHtmlContent() {
        String htmlContent = content.getText();
        htmlContent = initFonts(htmlContent); // TODO use velocity template engine

        hintTextPane.setContentType(content.getTypeId());
        hintTextPane.setText(htmlContent);
    }

    private void initEmptyContent() {
        hintTextPane.setContentType(MimeType.TEXT_PLAIN.id());
        hintTextPane.setText("");
    }

    public void setMessageType(MessageType messageType) {
        if (messageType == null) {
            iconPanel.setVisible(false);
            return;
        }

        Icon icon = getIcon(messageType);
        iconLabel.setIcon(icon);
    }

    private static Icon getIcon(MessageType messageType) {
        switch (messageType) {
            case INFO: return Icons.COMMON_INFO;
            case WARNING: return Icons.COMMON_WARNING;
            case ERROR: return Icons.COMMON_ERROR;
            default: return null;
        }
    }
}
