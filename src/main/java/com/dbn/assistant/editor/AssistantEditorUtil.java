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

package com.dbn.assistant.editor;

import com.dbn.assistant.DatabaseAssistantManager;
import com.dbn.assistant.state.AssistantState;
import com.dbn.common.action.Lookups;
import com.dbn.common.util.Documents;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.database.DatabaseFeature.AI_ASSISTANT;

/**
 * Database assistant utility class for editor actions
 * Features fast context lookup utilities like connection, chat-box state and prompt text candidate
 *
 * @author Dan Cioca (Oracle)
 */
@UtilityClass
public class AssistantEditorUtil {

    @Nullable
    public static AssistantState getAssistantState(@NotNull AnActionEvent e) {
        ConnectionHandler connection = getConnection(e);
        return getAssistantState(connection);
    }

    @Nullable
    private static AssistantState getAssistantState(@Nullable ConnectionHandler connection) {
        if (isNotValid(connection)) return null;

        Project project = connection.getProject();
        DatabaseAssistantManager assistantManager = DatabaseAssistantManager.getInstance(project);
        return assistantManager.getAssistantState(connection.getConnectionId());
    }

    public static boolean isAssistantAvailable(@NotNull AnActionEvent e) {
        AssistantState state = getAssistantState(e);
        if (state == null) return false;

        return state.isAvailable();
    }


    public static boolean isAssistantAvailable(@Nullable Editor editor) {
        if (isNotValid(editor)) return false;

        ConnectionHandler connection = getConnection(editor);
        AssistantState state = getAssistantState(connection);
        if (isNotValid(state)) return false;

        return state.isAvailable();
    }

    @Nullable
    public static ConnectionHandler getConnection(@Nullable Editor editor) {
        if (isNotValid(editor)) return null;

        Project project = editor.getProject();
        VirtualFile file = Documents.getVirtualFile(editor);
        return getConnection(project, file);
    }

    /**
     * Returns the connection in context of a given event
     * (only returns a connection if it supports AI_ASSISTANT feature)
     *
     * @param e the {@link AnActionEvent} to source context from
     * @return the {@link ConnectionHandler} associated to the editor in the context
     */
    @Nullable
    public static ConnectionHandler getConnection(@NotNull AnActionEvent e) {
        VirtualFile file = Lookups.getVirtualFile(e);
        Project project = Lookups.getProject(e);
        return getConnection(project, file);
    }

    @Nullable
    private static ConnectionHandler getConnection(@Nullable Project project, @Nullable VirtualFile file) {
        if (isNotValid(project)) return null;
        if (isNotValid(file)) return null;

        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
        ConnectionHandler connection = contextManager.getConnection(file);
        if (isNotValid(connection)) return null;
        if (!AI_ASSISTANT.isSupported(connection)) return null;

        return connection;
    }
}
