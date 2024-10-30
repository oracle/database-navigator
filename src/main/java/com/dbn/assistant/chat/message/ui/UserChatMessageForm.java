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

import com.dbn.assistant.chat.message.ChatMessage;
import com.dbn.assistant.chat.message.action.AskAgainAction;
import com.dbn.assistant.chat.message.action.CopyContentAction;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

public class UserChatMessageForm extends ChatMessageForm {
    private JPanel mainPanel;
    private JProgressBar progressBar;
    private JPanel actionPanel;
    private JTextPane messageTextPane;

    public UserChatMessageForm(ChatBoxForm parent, ChatMessage message) {
        super(parent, message);
        messageTextPane.setText(message.getContent());

        initActionToolbar();
        initProgressBar();
    }

    private void initProgressBar() {
        ChatMessage message = getMessage();
        progressBar.setVisible(message.isProgress());
        progressBar.setIndeterminate(true);
        progressBar.setBorder(JBUI.Borders.empty(0, 8, 8, 8));
    }

    @Override
    protected AnAction[] createActions() {
        String content = getMessage().getContent();
        return new AnAction[]{new AskAgainAction(content), new CopyContentAction(content)};
    }

    private void createUIComponents() {
        mainPanel = createMainPanel();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    protected JPanel getActionPanel() {
        return actionPanel;
    }

    @Override
    protected Color getBackground() {
        return Backgrounds.USER_PROMPT;
    }
}
