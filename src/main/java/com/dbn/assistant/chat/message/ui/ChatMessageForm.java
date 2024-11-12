/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * This software is dual-licensed to you under the Universal Permissive License
 * (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl or Apache License
 * 2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
 * either license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.dbn.assistant.chat.message.ui;

import com.dbn.assistant.chat.message.AuthorType;
import com.dbn.assistant.chat.message.ChatMessage;
import com.dbn.assistant.chat.message.ChatMessageContext;
import com.dbn.assistant.chat.message.action.CopyContentAction;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.common.color.Colors;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Fonts;
import com.dbn.common.util.Actions;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Stub implementation for chat message forms
 * Exposes logic shared by all different chat message types (USER, AGENT, SYSTEM)
 *
 * @author Dan Cioca (Oracle)
 */
@Getter
public abstract class ChatMessageForm extends DBNFormBase {
    protected interface Backgrounds {
        Color USER_PROMPT = new JBColor(new Color(218, 234, 255), new Color(68, 95, 128));
        Color AGENT_RESPONSE = Colors.delegate(() -> Colors.lafDarker(Colors.getPanelBackground(), 2));
        Color SYSTEM_INFO = Colors.delegate(() -> Colors.lafBrighter(Colors.getPanelBackground(), 2));
        Color SYSTEM_ERROR = new JBColor(new Color(255, 213, 204), new Color(69, 48, 43));
    }
    private final ChatMessage message;

    public ChatMessageForm(@Nullable Disposable parent, ChatMessage message) {
        super(parent);
        this.message = message;
    }

    @NotNull
    public static ChatMessageForm create(ChatBoxForm parent, ChatMessage message) {
        AuthorType author = message.getAuthor();
        switch (author) {
            case USER: return new UserChatMessageForm(parent, message);
            case AGENT: return new AgentChatMessageForm(parent, message);
            case SYSTEM: return new SystemChatMessageForm(parent, message);
            default: throw new IllegalArgumentException("Unknown author: " + author);
        }
    }

    protected abstract Color getBackground();

    protected void initTitlePanel() {
        JLabel titleLabel = getTitleLabel();
        if (titleLabel == null) return;

        ChatMessage message = getMessage();
        ChatMessageContext context = message.getContext();
        String title =
                context.getProfile() + " / " +
                        context.getModel() + "  -  " +
                        context.getAction().getName();

        titleLabel.setFont(Fonts.smaller(Fonts.deriveFont(Fonts.getLabelFont(), Font.BOLD), 2));
        titleLabel.setForeground(Colors.delegate(Colors::getLabelForeground));
        titleLabel.setText(title);
    }

    protected void initActionToolbar() {
        JPanel actionPanel = getActionPanel();
        ActionToolbar actionToolbar = Actions.createActionToolbar(actionPanel, "", true, createActions());

        JComponent component = actionToolbar.getComponent();
        component.setOpaque(false);
        component.setBorder(Borders.EMPTY_BORDER);
        actionPanel.add(component, BorderLayout.NORTH);
        actionPanel.setBorder(JBUI.Borders.empty(4));
    }

    protected AnAction[] createActions() {
        return new AnAction[]{new CopyContentAction(message.getContent())};
    }

    @Nullable
    protected JLabel getTitleLabel() {
        return null; // override if title creation is
    }
    protected abstract JPanel getActionPanel();

    /**
     * Custom painted JPanel to be used as rounded-corner container for chatbox messages
     * This utility is to be used for all chat message form implementations
     * for the creation of the main component (called "mainPanel" in all DBNForm components)
     */
    JPanel createMainPanel()  {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBackground(getBackground());
        return panel;
    }
}
