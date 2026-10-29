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

package com.dbn.common.ui.form;

import com.dbn.common.color.Colors;
import com.dbn.common.text.MimeType;
import com.dbn.common.text.TextContent;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.util.LookAndFeel;
import com.intellij.ui.BrowserHyperlinkListener;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.RoundedLineBorder;
import com.intellij.ui.components.JBLabel;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import static com.dbn.common.color.Colors.getContextHelpForeground;
import static com.dbn.common.color.Colors.getLabelForeground;
import static com.dbn.common.ui.link.Hyperlinks.onHyperlinkAccess;
import static com.dbn.common.ui.util.ClientProperty.RESIZING;
import static com.dbn.common.ui.util.UserInterface.adjustDimension;

public class DBNHintForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel contentPanel;
    private JTextPane hintTextPane;
    private HyperlinkLabel actionLink;
    private JBLabel hintLabel;

    private TextContent content;

    public DBNHintForm(DBNForm parent, @Nullable TextContent content) {
        this(parent, content, null, null);
    }

    public DBNHintForm(
            DBNForm parent,
            @Nullable TextContent content,
            String actionText,
            Runnable action) {
        super(parent);
        initActionLink(actionText, action);
        initHintTextPane();
        initHintLabel();
        setHintContent(content);

        updateComponentColors();
        mainPanel.setBorder(new RoundedLineBorder(Colors.getOutlineColor(), 2));
        //mainPanel.setBorder(new RoundedLineBorder(UIManager.getColor("TextField.borderColor"), 3));
        //mainPanel.setBorder(UIUtil.getTextFieldBorder());

        mainPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeComponent();
            }
        });
    }

    public static void installHintForm(DBNForm parent, @Nullable TextContent textContent, JPanel hintPanel) {
        new DBNHintForm(parent, textContent).installOn(hintPanel);
    }

    public void installOn(JPanel panel) {
        panel.removeAll();
        panel.setLayout(new BorderLayout());
        panel.add(getComponent());
    }

    private void initActionLink(String actionText, Runnable action) {
        if (actionText != null) {
            actionLink.setVisible(true);
            actionLink.setHyperlinkText(actionText);
            onHyperlinkAccess(actionLink, e -> action.run());
        } else {
            actionLink.setVisible(false);
        }
    }

    private void initHintLabel() {
        hintLabel.setText("");
        hintLabel.setVisible(false);
        hintLabel.setAllowAutoWrapping(true);
        hintLabel.setCopyable(true);
        hintLabel.setOpaque(false);
        hintLabel.addPropertyChangeListener(e -> {
            if ("font".equals(e.getPropertyName())) resizeComponent();
        });
    }

    private void initHintTextPane() {
        // workaround to force the text pane to resize to fit the content (alternative suggestions welcome)
        hintTextPane.setPreferredSize(new Dimension(-1, 500));
        hintTextPane.addPropertyChangeListener(e -> {
            if ("font".equals(e.getPropertyName())) {
                // appearance: accessibility zoom changes
                updateHintContent();
            }
        });

        hintTextPane.addHyperlinkListener(new BrowserHyperlinkListener());
    }

    private void updateComponentColors() {
        Color background = getBackground();
        Color foreground = getForeground();
        contentPanel.setBackground(background);
        contentPanel.setForeground(foreground);
        hintLabel.setForeground(foreground);
        hintTextPane.setForeground(foreground);
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
        if (!hintTextPane.isVisible()) {
            hintLabel.revalidate();

            Dimension contentPreferredSize = contentPanel.getPreferredSize();
            mainPanel.setPreferredSize(adjustDimension(contentPreferredSize, 0, 10));
            mainPanel.revalidate();
            return;
        }

        Dimension preferredSize = hintTextPane.getPreferredSize();
        hintTextPane.revalidate();

        Dimension contentPreferredSize = contentPanel.getPreferredSize();
        mainPanel.setPreferredSize(adjustDimension(contentPreferredSize, 0, 10));
        mainPanel.revalidate();

        Dimension contentSize = adjustDimension(preferredSize, 4, 4);
        hintTextPane.setPreferredSize(contentSize);
        hintTextPane.revalidate();
    }


    private static @NotNull Color getForeground() {
        return getLabelForeground();//getContextHelpForeground();
    }

    @NotNull
    private Color getBackground() {
        return LookAndFeel.isDarkMode() ?
                Colors.lafDarker(Colors.getPanelBackground(), 1) :
                Colors.lafBrighter(Colors.getPanelBackground(), 1);
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
        setContent();

        whenSettingsChange(() -> resetContent());
    }

    private void initHtmlContent() {
        content.initFonts(getForeground());
        setContent();

        whenSettingsChange(() -> resetContent());
    }

    private void resetContent() {
        content.rebuild();
        if (content.isHtml()) content.initFonts(getForeground());
        setContent();
    }

    private void setContent() {
        String text = content.getText();
        if (content.getType() == MimeType.TEXT_PLAIN && isLabelContent(text)) {
            hintTextPane.setVisible(false);
            hintLabel.setText(text);
            hintLabel.setVisible(true);
        } else {
            hintLabel.setVisible(false);
            hintTextPane.setVisible(true);
            hintTextPane.setContentType(content.getTypeId());
            String displayText = content.getType() == MimeType.TEXT_PLAIN ? normalizeLineBreaks(text) : text;
            hintTextPane.setText(displayText);
        }
    }

    private static boolean containsLineBreak(String text) {
        return text != null && (text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0);
    }

    private static boolean isLabelContent(String text) {
        if (containsLineBreak(text)) return false;
        if (text == null) return true;

        String trimmedText = text.trim();
        return !trimmedText.regionMatches(true, 0, "<html", 0, 5);
    }

    private static String normalizeLineBreaks(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }

    private void initEmptyContent() {
        hintTextPane.setVisible(false);
        hintLabel.setVisible(false);
        hintTextPane.setContentType(MimeType.TEXT_PLAIN.id());
        hintTextPane.setText("");
    }
}
