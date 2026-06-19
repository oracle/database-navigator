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

package com.dbn.common.ui.messages;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.message.MessageType;
import com.dbn.common.message.TitledMessage;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.ui.text.HiddenCaret;
import com.dbn.common.ui.util.Accessibility;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Fonts;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.NlsContexts.DialogMessage;
import com.intellij.openapi.util.NlsContexts.DialogTitle;
import com.intellij.ui.MouseDragHelper;
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;

import static com.dbn.common.message.MessageType.PROCESSING;
import static com.dbn.common.ui.link.Hyperlinks.onHyperlinkAccess;
import static com.dbn.nls.NlsResources.txt;
import static com.intellij.util.ui.JBUI.emptyInsets;

public class DBNMessageForm extends DBNFormBase {
    private static final String MESSAGE_DETAILS_MARKER = "\u2063";

    private JPanel mainPanel;
    private JLabel titleLabel;
    private JTextPane messageTextPane;
    private JTextPane detailsTextPane;
    private DBNHyperlinkLabel detailsToggle;
    private JPanel rememberOptionPanel;
    private JPanel iconPanel;

    private Icon icon;
    private AsyncProcessIcon processIcon;
    private @DialogTitle String title;
    private @DialogMessage String message;
    private @DialogMessage String collapsedMessage;
    private @DialogMessage String expandedMessage;

    public DBNMessageForm(@NotNull DBNComponent parent, TitledMessage message) {
        this(parent,
                message.getDialogIcon(),
                message.getTitle(),
                message.getText());
    }
    public DBNMessageForm(@NotNull DBNComponent parent, Icon icon, @DialogTitle String title, @DialogMessage String message) {
        super(parent);
        this.icon = icon;
        this.title = title;
        this.message = message;

        initIcon();
        initTitle();
        initDetailsToggle();
        initMessage();
        initDragging();
    }

    public void setMessage(TitledMessage message) {
        MessageType messageType = message.getType();
        if (messageType == PROCESSING) {
            initProgressIcon();
        } else {
            icon = message.getDialogIcon();
            initIcon();
        }

        setTitle(message.getTitle());
        setMessage(message.getText());
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
        initIcon();
    }

    public void setTitle(@DialogTitle String title) {
        this.title = title;
        initTitle();
    }

    public void setMessage(@DialogMessage String message) {
        this.message = message;
        initMessage();
    }

    private void initProgressIcon() {
        Disposer.dispose(processIcon);
        processIcon = new AsyncProcessIcon.Big("Processing");
        iconPanel.removeAll();
        iconPanel.add(processIcon);
        iconPanel.setVisible(true);
    }

    private void initIcon() {
        Disposer.dispose(processIcon);
        if (icon == null) {
            iconPanel.setVisible(false);
        } else {
            iconPanel.removeAll();
            iconPanel.setVisible(true);

            JLabel iconLabel = new JLabel();
            iconLabel.setIcon(icon);
            iconLabel.setText("");
            iconLabel.setPreferredSize(new Dimension(32, 32));
            iconPanel.add(iconLabel);
        }
    }

    private void initTitle() {
        titleLabel.setText(title);
        titleLabel.setFont(Fonts.regularBold(2));
    }

    private void initMessage() {
        boolean htmlMessage = isHtmlMessage(message);
        initTextPane(messageTextPane, htmlMessage);
        initTextPane(detailsTextPane, htmlMessage);

        initExpandableMessage(htmlMessage);
        updateMessageText();
        initCaretReset(messageTextPane);
        initCaretReset(detailsTextPane);
    }

    private void initTextPane(JTextPane textPane, boolean htmlMessage) {
        textPane.setCaret(new HiddenCaret());
        textPane.setContentType(htmlMessage ? "text/html" : "text/plain");
    }

    private void initCaretReset(JTextPane textPane) {
        textPane.setCaretPosition(0);
        textPane.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                textPane.setCaretPosition(0);
            }
        });
    }

    private void initDetailsToggle() {
        onHyperlinkAccess(detailsToggle, e -> {
            detailsTextPane.setVisible(true);
            detailsTextPane.setCaretPosition(0);
            detailsToggle.setVisible(false);
        });
    }

    private void initExpandableMessage(boolean htmlMessage) {
        expandedMessage = normalizeLineBreaks(message);
        collapsedMessage = expandedMessage;

        if (htmlMessage || expandedMessage == null) {
            detailsToggle.setVisible(false);
            return;
        }

        int detailsMarkerIndex = expandedMessage.indexOf(MESSAGE_DETAILS_MARKER);
        if (detailsMarkerIndex == -1) {
            detailsToggle.setVisible(false);
            return;
        }

        expandedMessage = expandedMessage.replace(MESSAGE_DETAILS_MARKER, "");
        int detailsBreakIndex = expandedMessage.indexOf("\n\n", detailsMarkerIndex + 1);
        if (detailsBreakIndex == -1) {
            collapsedMessage = expandedMessage;
            detailsToggle.setVisible(false);
            return;
        }

        collapsedMessage = expandedMessage.substring(0, detailsBreakIndex).trim();
        expandedMessage = "\n" + expandedMessage.substring(detailsBreakIndex).trim();
        detailsToggle.setVisible(true);
    }

    private void updateMessageText() {
        messageTextPane.setText(collapsedMessage);
        messageTextPane.setCaretPosition(0);
        detailsTextPane.setText(expandedMessage);
        detailsTextPane.setCaretPosition(0);
        detailsTextPane.setVisible(false);
        detailsToggle.setHyperlinkText(txt("app.shared.link.ShowMore"));
    }

    private static @Nullable String normalizeLineBreaks(@Nullable String message) {
        return message == null ? null : message.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static boolean isHtmlMessage(@Nullable String message) {
        return message != null && message.trim().startsWith("<html>");
    }

    void initRememberOption(@Nullable JCheckBox checkBox) {
        if (checkBox == null) {
            rememberOptionPanel.setVisible(false);
        } else {
            checkBox.setBorder(Borders.EMPTY_BORDER);
            checkBox.setMargin(emptyInsets());
            rememberOptionPanel.add(checkBox, BorderLayout.WEST);
        }
    }

    protected void initAccessibility() {
        Accessibility.setAccessibleDescription(messageTextPane, txt("app.shared.aria.Message"));
    }

    private void initDragging() {
        Disposable parentComponent = getParentComponent();
        if (parentComponent instanceof DBNMessageDialog dialog) {
            DragHelper dragHelper = new DragHelper(dialog, this);
            dragHelper.start();
        }
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    private static class DragHelper extends MouseDragHelper<JPanel> {
        private final DBNMessageDialog dialog;
        private Point location;

        public DragHelper(@NotNull DBNMessageDialog dialog, DBNMessageForm form) {
            super(dialog, form.mainPanel);
            this.dialog = dialog;
        }

        @Override
        protected boolean canStartDragging(@NotNull JComponent dragComponent, @NotNull Point dragComponentPoint) {
            Component target = dragComponent.findComponentAt(dragComponentPoint);
            return target == null || target == dragComponent || target instanceof JPanel;
        }

        @Override
        protected void processDrag(@NotNull MouseEvent event, @NotNull Point dragToScreenPoint, @NotNull Point startScreenPoint) {
            Window window = dialog.getWindow();
            if (location == null) {
                location = window.getLocation();
            }
            window.setLocation(new Point(
                location.x + dragToScreenPoint.x - startScreenPoint.x,
                location.y + dragToScreenPoint.y - startScreenPoint.y));
        }

        @Override
        protected void processDragCancel() {
            location = null;
        }

        @Override
        protected void processDragFinish(@NotNull MouseEvent event, boolean willDragOutStart) {
            location = null;
        }

        @Override
        protected void processDragOutFinish(@NotNull MouseEvent event) {
            location = null;
        }

        @Override
        protected void processDragOutCancel() {
            location = null;
        }

        @Override
        protected void processDragOut(@NotNull MouseEvent event, @NotNull Point dragToScreenPoint, @NotNull Point startScreenPoint, boolean justStarted) {
            super.processDragOut(event, dragToScreenPoint, startScreenPoint, justStarted);
            location = null;
        }
    }
}
