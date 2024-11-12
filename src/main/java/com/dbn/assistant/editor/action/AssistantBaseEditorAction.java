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

package com.dbn.assistant.editor.action;

import com.dbn.assistant.chat.window.PromptAction;
import com.dbn.assistant.editor.AssistantEditorAdapter;
import com.dbn.assistant.editor.AssistantPrompt;
import com.dbn.assistant.editor.AssistantPromptUtil;
import com.dbn.assistant.state.AssistantState;
import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.ProjectAction;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import static com.dbn.assistant.editor.AssistantEditorUtil.getAssistantState;
import static com.dbn.assistant.editor.AssistantEditorUtil.getConnection;
import static com.dbn.assistant.editor.AssistantEditorUtil.isAssistantAvailable;
import static com.dbn.assistant.editor.AssistantPromptUtil.isAssistantPromptAvailable;
import static com.dbn.common.action.Lookups.getEditor;
import static com.dbn.common.dispose.Checks.isNotValid;

/**
 * Action stub for DB-Assistant editor context menu
 *
 * @author Ayoub Aarrasse (Oracle)
 * @author Dan Cioca (Oracle)
 */
@BackgroundUpdate
public abstract class AssistantBaseEditorAction extends ProjectAction {

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        Editor editor = getEditor(e);
        if (isNotValid(editor)) return;

        ConnectionHandler connection = getConnection(e);
        if (isNotValid(connection)) return;

        AssistantPrompt prompt = AssistantPromptUtil.resolveAssistantPrompt(editor, null);
        if (prompt == null) return;

        AssistantEditorAdapter.submitQuery(project, editor, connection.getConnectionId(), prompt.getText(), getAction());
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Editor editor = getEditor(e);
        boolean visible = isAssistantAvailable(e);
        boolean enabled = isAssistantPromptAvailable(editor, null);

        ActionPlace actionPlace = getActionPlace(e);
        String actionText = getActionGroupName(e) + getActionName(actionPlace);

        Presentation presentation = e.getPresentation();
        presentation.setVisible(visible);
        presentation.setEnabled(enabled);
        presentation.setText(actionText);
    }

    private String getActionGroupName(@NotNull AnActionEvent e){
        AssistantState state = getAssistantState(e);
        if (isNotValid(state)) return "";

        return state.getAssistantName() + " - ";
    }

    /**
     * Returns the place where the action is being invoked
     *  - EDITOR_POPUP_MENU when action is shown in the context menu of the editor (right-click)
     *  - GENERATE_ACTION_GROUP when action is invoked in the "Generate (code)" utility
     * @param e the {@link AnActionEvent} to resolve context of the action
     * @return the {@link ActionPlace} where action has been invoked / shown
     */
    private ActionPlace getActionPlace(@NotNull AnActionEvent e) {
        boolean editorPopup = Strings.containsIgnoreCase(e.getPlace(), "EditorPopup");
        return editorPopup ? ActionPlace.EDITOR_POPUP_MENU : ActionPlace.GENERATE_ACTION_GROUP;
    }

    protected abstract PromptAction getAction();

    @Nls
    protected abstract String getActionName(ActionPlace place);

    protected enum ActionPlace {
        GENERATE_ACTION_GROUP,
        EDITOR_POPUP_MENU
    }
}
