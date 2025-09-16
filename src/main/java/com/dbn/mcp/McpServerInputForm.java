package com.dbn.mcp;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.mcp.models.ToolDefinitionModel;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.LinkedHashMap;
import java.util.Map;

public class McpServerInputForm  extends DBNFormBase {

  private JPanel mainPanel;
  private JTextField urlTextField;
  private JTextField usernameTextField;
  private JTextField passwordTextField;
  private JPanel hintPanel;
  private JPanel toolDefinitionPanel;
  private ToolDefinitionListForm toolDefinitionListForm;

  public McpServerInputForm(@Nullable Disposable parent) {
    super(parent);
    initHintPanel();
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
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





  public String getJdbcUrl() {
    return urlTextField != null ? urlTextField.getText().trim() : "";
  }

  public String getUsername() {
    return usernameTextField != null ? usernameTextField.getText().trim() : "";
  }

  public char[] getPassword() {
    return passwordTextField != null ? passwordTextField.getText().toCharArray() : new char[0];
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