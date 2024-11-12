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

package com.dbn.assistant.chat.window.action;

import com.dbn.assistant.chat.window.PromptAction;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.common.action.DataKeys;
import com.dbn.common.action.ToggleAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Action for selecting the type of interaction with the AI-assistant engine
 *
 * @author Dan Cioca (Oracle)
 */
public class TypeSelectAction extends ToggleAction {
    private final PromptAction type;

    TypeSelectAction(PromptAction type) {
        super(type.getName(), type.getDescription(), null);
        this.type = type;
        getTemplatePresentation().setIcon(null);
    }

    @Override
    public boolean displayTextInToolbar() {
        return true;
    }

    public static class ShowSQL extends TypeSelectAction {
        public ShowSQL() {
            super(PromptAction.SHOW_SQL);
        }
    }

    public static class ExplainSQL extends TypeSelectAction {
        public ExplainSQL() {
            super(PromptAction.EXPLAIN_SQL);
        }
    }

    public static class Narrate extends TypeSelectAction {
        public Narrate() {
            super(PromptAction.NARRATE);
        }
    }

    public static class Chat extends TypeSelectAction {
        public Chat() {
            super(PromptAction.CHAT);
        }
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        ChatBoxForm chatBox = e.getData(DataKeys.ASSISTANT_CHAT_BOX);
        if (chatBox == null) return false;

        PromptAction action = chatBox.getAssistantState().getSelectedAction();
        return action == type;
    }

    private static boolean isEnabled(@NotNull AnActionEvent e) {
        ChatBoxForm chatBox = e.getData(DataKeys.ASSISTANT_CHAT_BOX);
        if (chatBox == null) return false;

        return chatBox.isPromptingAvailable();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        if (!state) return;

        ChatBoxForm chatBox = e.getData(DataKeys.ASSISTANT_CHAT_BOX);
        if (chatBox == null) return;

        chatBox.selectAction(type);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);

        boolean enabled = isEnabled(e);
        e.getPresentation().setEnabled(enabled);
    }
}
