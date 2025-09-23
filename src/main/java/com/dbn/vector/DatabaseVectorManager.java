package com.dbn.vector;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.Components;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.routine.Consumer;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseAssistantInterface;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.object.DBSchema;
import com.dbn.vector.model.chunk.ChunkConfiguration;
import com.dbn.vector.model.embed.EmbedConfig;
import com.dbn.vector.model.sourceconfig.DBTableSourceConfig;
import com.dbn.vector.model.sourceconfig.FileSystemSourceConfig;
import com.dbn.vector.model.sourceconfig.SourceConfig;
import com.dbn.vector.model.store.StoreConfig;
import com.dbn.vector.ui.VectorAiDialog;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import groovy.util.logging.Slf4j;
import lombok.SneakyThrows;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.io.FileReader;
import java.io.Reader;
import java.io.Writer;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import static com.dbn.common.Priority.HIGHEST;
import static com.dbn.common.Priority.MEDIUM;
import static com.dbn.common.ui.CardLayouts.addCard;
import static com.dbn.common.ui.CardLayouts.isBlankCard;
import static com.dbn.common.ui.CardLayouts.showCard;
import static com.dbn.common.ui.CardLayouts.visibleCardId;


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

//        VectorAIForm chatBox = new VectorAIForm(ConnectionHandler.get(connectionId));
        JButton vectorAiButton = new JButton("Vector AI");
        vectorAiButton.addActionListener(e -> {
            Dialogs.show(()->new VectorAiDialog(getProject(),"Vector Ai",true,ConnectionHandler.get(connectionId)));
        });
        addCard(toolWindowPanel, vectorAiButton, connectionId);

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

    public ResultSet chunkTextContent(ConnectionHandler connection, ChunkConfiguration configuration, String text) throws SQLException {
        return DatabaseInterfaceInvoker.load(MEDIUM,
                "Chunking Data",
                "Chunking text content",
                connection.getProject(),
                connection.getConnectionId(),
                conn -> {
                    DatabaseAssistantInterface assistantInterface = connection.getAssistantInterface();
                    return assistantInterface.chunk(text, configuration, conn);
                });
    }

    @SneakyThrows
    public void query(SourceConfig sourceConfig, ChunkConfiguration chunkConfiguration, EmbedConfig embedConfig, StoreConfig storeConfig, ConnectionHandler handler, Runnable callbackInfo, Consumer<Exception> callbackError) throws SQLException {
        Progress.modal(
                getProject(),
                handler.getSchema(), true,
                "Embedding Data",
                "Creating store table" + storeConfig.getTableName(),
                 p -> {
                    try {
                        DatabaseInterfaceInvoker.execute(HIGHEST,
                                p.getText(),
                                p.getText2(),
                                handler.getProject(),
                                handler.getConnectionId(),
                                handler.getSchemaId(),
                                conn -> {
                                    DBSchema schema = handler.getSchema(handler.getUserSchema());

                                    DatabaseAssistantInterface dataDefinition = schema.getAssistantInterface();
                                    dataDefinition.createTable(conn, storeConfig.getTableName());
                                    p.setText2("Embedding data");
                                    if (sourceConfig instanceof DBTableSourceConfig) {
                                        dataDefinition.embed(conn, (DBTableSourceConfig) sourceConfig, chunkConfiguration, embedConfig, storeConfig);
                                        System.out.println("Embedding data created");
                                    } else {
                                      Clob clob = prepareFileClob(conn,((FileSystemSourceConfig)sourceConfig).getVirtualFiles().get(0));
                                        dataDefinition.embed(conn,clob,chunkConfiguration,
                                                             embedConfig,storeConfig );
                                      System.out.println("Embedding data created");

                                    }
                                    callbackInfo.run();
                                });
                    }catch (SQLException e) {
                      callbackError.accept(e);
//                      new RuntimeException(e);
                    }
                });

    }

  private Clob prepareFileClob(DBNConnection conn,VirtualFile virtualFile)  {
    try{
      Clob docClob = conn.createClob();
      String path = virtualFile.getPath();
      Reader reader = new FileReader(path);
      Writer writer = docClob.setCharacterStream(1);

      char[] buffer = new char[8192];
      int len;
      while ((len = reader.read(buffer)) != -1) {
        writer.write(buffer, 0, len);
      }

      return docClob;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }


  }
}
