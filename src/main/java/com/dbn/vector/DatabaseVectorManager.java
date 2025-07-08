package com.dbn.vector;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.Components;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SessionId;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseAssistantInterface;
import com.dbn.vector.model.ChunkData;
import com.dbn.vector.ui.ChunkConfiguration;
import com.dbn.vector.ui.VectorAIForm;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import groovy.util.logging.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.ui.CardLayouts.*;


@Slf4j
@State(
        name = DatabaseVectorManager.COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public  class DatabaseVectorManager extends ProjectComponentBase implements PersistentState {
    public static final String TOOL_WINDOW_ID = "DB Vector";
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseVectorManager";




    public DatabaseVectorManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
    }

    public static DatabaseVectorManager getInstance(Project project) {
        return Components.projectService(project, DatabaseVectorManager.class);
    }

    /**
     * switch from current connection to the new selected one from DBN navigator
     *
     * @param connectionId the new selected connection
     */
    public void switchToConnection(@Nullable ConnectionId connectionId) {
        JPanel toolWindowPanel = getToolWindowPanel();
        if (toolWindowPanel == null) return;

        String id = visibleCardId(toolWindowPanel);
        ConnectionId selectedConnectionId = isBlankCard(id) ? null : ConnectionId.get(id);

        if (Objects.equals(selectedConnectionId, connectionId)) return;
        initToolWindow(connectionId);
    }


    private void initToolWindow(ConnectionId connectionId) {
        ToolWindow toolWindow = getToolWindow();
        if (toolWindow == null) return;

        JPanel toolWindowPanel = getToolWindowPanel();
        if (toolWindowPanel == null) return;

        VectorAIForm chatBox = new VectorAIForm(ConnectionHandler.get(connectionId));
        addCard(toolWindowPanel, chatBox, connectionId);

//        if (chatBox == null) {
        showCard(toolWindowPanel,connectionId);
        toolWindow.setAvailable(true);
//        } else {
//            showCard(toolWindowPanel, connectionId);
//            toolWindow.setAvailable(true);
//        }


    }

    @Nullable
    private JPanel getToolWindowPanel() {
        ToolWindow toolWindow = getToolWindow();
        if (toolWindow == null) return null;

        Content content = toolWindow.getContentManager().getContent(0);
        return content == null ? null : (JPanel) content.getComponent();
    }

    @Nullable
    public ToolWindow getToolWindow() {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(getProject());
        return toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
    }

    @Override
    public Element getComponentState() {
        return null;
    }

    @Override
    public void loadComponentState(@NotNull Element state) {

    }

  public List<ChunkData> chunk(ChunkConfiguration chunkConfiguration, String text, DBNConnection conn) throws SQLException {
//      DBNConnection conn = connectionHandler.getConnection(SessionId.POOL);

      DatabaseAssistantInterface assistantInterface = conn.getConnectionHandler().getAssistantInterface();
      ResultSet resultSet = assistantInterface.chunk(text,chunkConfiguration,conn);
      List<ChunkData> chunks = new ArrayList<>();

      while (resultSet.next()) {
          long chunkOffset = resultSet.getLong("CHUNK_OFFSET");
          long chunkLength = resultSet.getLong("CHUNK_LENGTH");
          String chunkText = resultSet.getString("CHUNK_TEXT");

          chunks.add(new ChunkData(chunkOffset,chunkLength,chunkText));
      }

      return chunks;



  }
}
