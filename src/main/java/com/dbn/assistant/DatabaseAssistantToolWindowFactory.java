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

package com.dbn.assistant;

import com.dbn.common.event.ProjectEvents;
import com.dbn.common.ui.CardLayouts;
import com.dbn.common.ui.window.DBNToolWindowFactory;
import com.dbn.common.util.Editors;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConsoleChangeListener;
import com.dbn.connection.mapping.FileConnectionContextListener;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static com.dbn.assistant.DatabaseAssistantManager.TOOL_WINDOW_ID;
import static com.dbn.common.icon.Icons.WINDOW_DATABASE_ASSISTANT;
import static com.dbn.nls.NlsResources.txt;

/**
 * Tool window factory for the Database AI-Assistant chat box
 *
 * @author Ayoub Aarrasse (Oracle)
 */
public class DatabaseAssistantToolWindowFactory extends DBNToolWindowFactory {

  @Override
  protected void initialize(@NotNull ToolWindow toolWindow) {
    toolWindow.setTitle(txt("companion.window.title"));
    toolWindow.setStripeTitle(txt("companion.window.title"));
    toolWindow.setIcon(WINDOW_DATABASE_ASSISTANT.get());
  }

  @Override
  public void createContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    createContentPanel(toolWindow);
    toolWindow.setToHideOnEmptyContent(true);
    toolWindow.setAutoHide(false);

    DatabaseAssistantManager manager = DatabaseAssistantManager.getInstance(project);
    ProjectEvents.subscribe(project, manager,
            ConsoleChangeListener.TOPIC,
            connectionId -> manager.switchToConnection(connectionId));

    ProjectEvents.subscribe(project, manager,
            FileConnectionContextListener.TOPIC,
            createConnectionContextListener());

    ProjectEvents.subscribe(project, manager,
            ToolWindowManagerListener.TOPIC,
            createToolWindowListener(project));
  }

  private static void createContentPanel(@NotNull ToolWindow toolWindow) {
    ContentManager contentManager = toolWindow.getContentManager();
    JPanel contentPanel = CardLayouts.createCardPanel(true);

    ContentFactory contentFactory = contentManager.getFactory();
    Content content = contentFactory.createContent(contentPanel, null, true);
    contentManager.addContent(content);
  }

  private static @NotNull FileConnectionContextListener createConnectionContextListener() {
    return new FileConnectionContextListener() {
      @Override
      public void connectionChanged(Project project, VirtualFile file, ConnectionHandler connection) {
        if (!file.isInLocalFileSystem()) return; // changing connection in surrogate (LightVirtualFiles) should not cause connection switch

        ConnectionId connectionId = connection == null ? null : connection.getConnectionId();
        DatabaseAssistantManager manager = DatabaseAssistantManager.getInstance(project);
        manager.switchToConnection(connectionId);
      }
    };
  }

  private static ToolWindowManagerListener createToolWindowListener(Project project) {
    return new ToolWindowManagerListener() {

      @Override
      public void stateChanged(@NotNull ToolWindowManager toolWindowManager) {
        ToolWindow toolWindow = toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
        if (toolWindow == null) return;
        if (!toolWindow.isVisible()) return;

        VirtualFile file = Editors.getSelectedFile(project);
        if (file == null) return;

        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
        ConnectionId connectionId = contextManager.getConnectionId(file);
        if (connectionId == null) return; // do not switch away from last selected connection

        DatabaseAssistantManager assistantManager = DatabaseAssistantManager.getInstance(project);
        assistantManager.switchToConnection(connectionId);
      }
    };
  }

}
