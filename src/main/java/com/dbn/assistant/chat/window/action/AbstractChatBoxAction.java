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

import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.assistant.state.AssistantState;
import com.dbn.common.action.DataKeys;
import com.dbn.common.action.ProjectAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract basic implementation for actions presented in the ChatBox
 * Features lookup utilities for the chat box component as well as the state of the assistant interface
 *
 * @author Dan Cioca (Oracle)
 */
public abstract class AbstractChatBoxAction extends ProjectAction {

    @Nullable
    protected static ChatBoxForm getChatBox(@NotNull AnActionEvent e) {
        return e.getData(DataKeys.ASSISTANT_CHAT_BOX);
    }

    @Nullable
    protected static AssistantState getAssistantState(@NotNull AnActionEvent e) {
        ChatBoxForm chatBox = getChatBox(e);
        return chatBox == null ? null : chatBox.getAssistantState();
    }
}
