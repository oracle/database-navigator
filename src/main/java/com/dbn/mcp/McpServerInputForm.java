package com.dbn.mcp;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.DatabaseType;
import com.dbn.mcp.models.ToolDefinitionModel;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;


public class McpServerInputForm extends DBNFormBase {

  private JPanel mainPanel;
  private DBNComboBox<ConnectionHandler> connectionComboBox;
  private JPanel hintPanel;
  private JPanel toolDefinitionPanel;
  private ToolDefinitionListForm toolDefinitionListForm;

  public McpServerInputForm(@Nullable Disposable parent) {
    super(parent);
    initHintPanel();
    initConnectionComboBox();
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  private void initConnectionComboBox() {
    Project project = getProject();
    if (project == null) return;
    
    ConnectionManager connectionManager = ConnectionManager.getInstance(project);
    List<ConnectionHandler> oracleConnections = connectionManager.getConnections(DatabaseType.ORACLE);
    
    connectionComboBox.setValues(oracleConnections);
    if (!oracleConnections.isEmpty()) {
      connectionComboBox.setSelectedValue(oracleConnections.get(0));
    }
  }

  private void initHintPanel() {

    TextContent hintText = TextContent.html(
            "<html>" +
                    "<div style='font-size:11px;margin:4px 0;'>" +
                    "<b>Build a governed MCP data tool</b> — turn one safe SQL into a ready-to-run MCP server JAR. " +
                    "Use <code>:param</code> names (<i>all required</i>), fill connection + tool info, then click <b>Build</b>. " +
                    "We’ll drop the JAR into <code>mcp-dist</code> and show the client config snippet." +
                    "</div>" +
                    "<ul style='margin-top:2px'>" +
                    "<li>Write SQL with <code>:param</code> (e.g., <code>:start_date</code>)</li>" +
                    "<li>Fill JDBC + tool details</li>" +
                    "<li>Hit <b>Build</b> → paste the snippet into your MCP client</li>" +
                    "</ul>" +
                    "</html>"
    );
    DBNHintForm hintForm = new DBNHintForm(null, hintText, null, true);

    JComponent hintComponent = hintForm.getComponent();
    hintPanel.add(hintComponent);
  }




  // Build helpers





  private void createUIComponents() {
    // TODO: place custom component creation code here
    System.out.println("HI");
    toolDefinitionListForm = new ToolDefinitionListForm(this);
    toolDefinitionPanel = (JPanel) toolDefinitionListForm.getComponent();
  }





  /**
   * Returns the selected connection from the combo box.
   * Callers should extract whatever information they need from the ConnectionHandler.
   * 
   * @return the selected ConnectionHandler, or null if none selected
   */
  @Nullable
  public ConnectionHandler getSelectedConnection() {
    return connectionComboBox != null ? connectionComboBox.getSelectedValue() : null;
  }

//  public String getToolName() {
//    return toolName != null ? toolName.getText().trim() : "";
//  }

//  public String getToolDescription() {
//    return toolDescriptionTextArea != null ? toolDescriptionTextArea.getText() : "";
//  }

//  public String getSql() {
//    return queryTextArea != null ? queryTextArea.getText() : "";
//  }

//  public List<ParamRow> getParams() {
//    return paramsModel != null ? new ArrayList<>(paramsModel.rows) : List.of();
//  }

  public enum ParamType {String, Integer, Float, Boolean, Date}

  public static class ParamRow {
    public String name;          // e.g., ":deptId"
    public ParamType type;
    public String defaultValue;

    public ParamRow(String name) {
      this(name, ParamType.String, "");
    }

    public ParamRow(String name, ParamType type, String defaultValue) {
      this.name = name;
      this.type = (type == null ? ParamType.String : type);
      this.defaultValue = defaultValue == null ? "" : defaultValue;
    }
  }

  public List<ToolDefinitionModel> getTools(){
    return toolDefinitionListForm.getToolDefinitionModelList();
  }
}