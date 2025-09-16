package com.dbn.mcp.ui;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.mcp.McpServerInputForm;
import com.dbn.mcp.models.ToolDefinitionModel;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ToolDefinitionCreateForm extends DBNFormBase {
  private JPanel mainPanel;
  private JTextField toolName;
  private com.intellij.ui.components.JBTextArea toolDescriptionTextArea;
  private JPanel parametersPanel;
  private JBTextArea queryTextArea;
  private JPanel helpIconPanel;

  private JTable paramsTable;
  private ParamTableModel paramsModel;
  private boolean suppressDocEvents = false;




  public ToolDefinitionCreateForm(@Nullable Disposable parent) {
    super(parent);

    if (paramsTable == null) {
      initParametersPanel();
      refreshFromSql();
      decorateTextAreas();
    }

    addParameterHelpIfNeeded();
  }

  private void addParameterHelpIfNeeded() {
    if (helpIconPanel == null) return;
    if (helpIconPanel.getComponentCount() > 0) return; // already added

    JPanel helpNorth = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    helpNorth.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
    JButton helpBtn = new JButton(AllIcons.General.ContextHelp);
    helpBtn.setToolTipText("How parameters work");
    helpBtn.setFocusable(false);
    helpBtn.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    helpBtn.setContentAreaFilled(false);
    helpBtn.addActionListener(e -> showParamHelpDialog());

    helpNorth.add(helpBtn);

    // Ensure layout and add to the designated panel area
    if (!(helpIconPanel.getLayout() instanceof BorderLayout)) {
      helpIconPanel.setLayout(new BorderLayout());
    }
    helpIconPanel.add(helpNorth, BorderLayout.EAST);
    helpIconPanel.revalidate();
    helpIconPanel.repaint();
  }

  private void showParamHelpDialog() {
    final String exampleSql =
            "SELECT e.name, e.salary\n" +
                    "FROM employees e\n" +
                    "WHERE e.dept_id = :dept\n" +
                    "  AND e.salary >= :min_salary;";

    Consumer<String> insertIntoQuery = sql -> {
      JTextArea ta = queryTextArea();
      if (ta != null) {
        ta.replaceSelection(sql);
        ta.requestFocusInWindow();
      }
    };

    new ParamHelpDialog(exampleSql, insertIntoQuery).show();
  }

  private void initParametersPanel() {
    paramsModel = new ParamTableModel();
    paramsTable = new JBTable(paramsModel);
    paramsTable.setFillsViewportHeight(true);
    paramsTable.setRowHeight(24);
    paramsTable.setDefaultEditor(McpServerInputForm.ParamType.class, new DefaultCellEditor(new JComboBox<>(McpServerInputForm.ParamType.values())));

    paramsTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteRow");
    paramsTable.getActionMap().put("deleteRow", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        removeSelectedParamFromSql();
      }
    });

    JScrollPane tableScroll = new JBScrollPane(paramsTable);
    tableScroll.setPreferredSize(new Dimension(650, 160));

    parametersPanel.setLayout(new BorderLayout(5, 5));
    JPanel helpNorth = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    helpNorth.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
    JButton helpBtn = new JButton(AllIcons.General.ContextHelp);
    helpBtn.setToolTipText("How parameters work");
    helpBtn.setFocusable(false);
    helpBtn.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    helpBtn.setContentAreaFilled(false);
    helpBtn.addActionListener(e -> {}/* showParamHelpDialog()*/);
    helpNorth.add(helpBtn);


    parametersPanel.add(tableScroll, BorderLayout.CENTER);

    if (paramsTable.getTableHeader() != null) {
      paramsTable.getTableHeader().setToolTipText("Name = :param in SQL • Type = JSON type • Default = optional value used if omitted");
    }

    JTextArea q = queryTextArea();
    if (q != null) {
      q.getDocument().addDocumentListener(new DocumentListener() {
        private void onChange() {
          if (suppressDocEvents) return;
          refreshFromSql();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
          onChange();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
          onChange();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
          onChange();
        }
      });
    }
  }

  private JTextArea queryTextArea() {
    return queryTextArea;
  }

  private void decorateTextAreas() {
    // Decorate SQL query textarea
    if (queryTextArea != null) {
      queryTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
      queryTextArea.setMargin(new Insets(6, 8, 6, 8));

      queryTextArea.setTabSize(2);
      queryTextArea.setLineWrap(false);
      queryTextArea.setToolTipText("Write SQL here. Use :param placeholders, e.g. WHERE dept_id = :dept");
      queryTextArea.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createTitledBorder("SQL Query (use :param placeholders, e.g., :dept)"),
              BorderFactory.createEmptyBorder(4, 6, 6, 6)
      ));
      // context menu: quick :param insert
      JPopupMenu menu = new JPopupMenu();
      JMenuItem insertParam = new JMenuItem("Insert :param…");
      insertParam.addActionListener(e -> {
        String name = JOptionPane.showInputDialog(queryTextArea, "Parameter name (without ':'):", "Insert :param", JOptionPane.PLAIN_MESSAGE);
        if (name != null) {
          name = name.trim();
          if (!name.isEmpty()) {
            queryTextArea.replaceSelection(":" + name);
            queryTextArea.requestFocusInWindow();
          }
        }
      });
      menu.add(insertParam);
      queryTextArea.setComponentPopupMenu(menu);
      // placeholder hint when empty
      installPlaceholder(queryTextArea, "SELECT ... WHERE dept_id = :dept AND salary >= :min_salary");
    }

    // Decorate tool description textarea
    if (toolDescriptionTextArea != null) {
      toolDescriptionTextArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
      toolDescriptionTextArea.setMargin(new Insets(6, 8, 6, 8));
      toolDescriptionTextArea.setLineWrap(true);
      toolDescriptionTextArea.setWrapStyleWord(true);
      toolDescriptionTextArea.setToolTipText("Brief description shown to the MCP client for this tool.");
      toolDescriptionTextArea.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createTitledBorder("Tool Description"),
              BorderFactory.createEmptyBorder(4, 6, 6, 6)
      ));
    }
  }

  private void installPlaceholder(final JTextArea ta, final String placeholder) {
    final Color hintColor = UIManager.getColor("Label.disabledForeground");
    final Color normalColor = UIManager.getColor("TextArea.foreground");
    if (ta.getText() == null || ta.getText().trim().isEmpty()) {
      ta.setForeground(hintColor);
      ta.setText(placeholder);
      ta.putClientProperty("placeholder-active", Boolean.TRUE);
    }
    ta.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        Object active = ta.getClientProperty("placeholder-active");
        if (Boolean.TRUE.equals(active)) {
          ta.setText("");
          ta.setForeground(normalColor);
          ta.putClientProperty("placeholder-active", Boolean.FALSE);
        }
      }
      @Override
      public void focusLost(FocusEvent e) {
        if (ta.getText().trim().isEmpty()) {
          ta.setForeground(hintColor);
          ta.setText(placeholder);
          ta.putClientProperty("placeholder-active", Boolean.TRUE);
        }
      }
    });
    ta.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
      private void update() {
        Object active = ta.getClientProperty("placeholder-active");
        if (ta.hasFocus()) return;
        if (ta.getText().trim().isEmpty() && !Boolean.TRUE.equals(active)) {
          ta.setForeground(hintColor);
          ta.setText(placeholder);
          ta.putClientProperty("placeholder-active", Boolean.TRUE);
        } else if (Boolean.TRUE.equals(active) && !placeholder.equals(ta.getText())) {
          ta.setForeground(normalColor);
          ta.putClientProperty("placeholder-active", Boolean.FALSE);
        }
      }
      @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
      @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
      @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
    });
  }

  private void removeSelectedParamFromSql() {
    int row = paramsTable.getSelectedRow();
    if (row < 0 || row >= paramsModel.getRowCount()) {
      Toolkit.getDefaultToolkit().beep();
      return;
    }
    String nameWithColon = paramsModel.rows.get(row).name; // e.g. :dept
    String name = stripColon(nameWithColon);
    JTextArea ta = queryTextArea();
    if (ta == null) return;
    String sql = ta.getText();
    // remove ALL occurrences of this named parameter
    String newSql = sql.replaceAll(":" + Pattern.quote(name) + "\\b", "");
    suppressDocEvents = true;
    try {
      ta.setText(newSql);
    } finally {
      suppressDocEvents = false;
    }
    refreshFromSql();
  }

  public static String stripColon(String s) {
    if (s == null) return "";
    return s.startsWith(":") ? s.substring(1) : s;
  }

  private void refreshFromSql() {
    JTextArea ta = queryTextArea();
    String sql = ta != null ? ta.getText() : "";
    List<String> occ = parseOccurrences(sql);
    List<String> uniq = uniqueInOrder(occ);

    Map<String, McpServerInputForm.ParamRow> existing = new LinkedHashMap<>();
    for (McpServerInputForm.ParamRow r : paramsModel.rows) {
      existing.put(stripColon(r.name), r);
    }

    paramsModel.rows.clear();
    for (String name : uniq) {
      McpServerInputForm.ParamRow prev = existing.get(name);
      if (prev != null) {
        paramsModel.rows.add(new McpServerInputForm.ParamRow(":" + name, prev.type, prev.defaultValue));
      } else {
        paramsModel.rows.add(new McpServerInputForm.ParamRow(":" + name, McpServerInputForm.ParamType.String, ""));
      }
    }
    paramsModel.fireTableDataChanged();
  }

  public static List<String> parseOccurrences(String sql) {
    List<String> occ = new ArrayList<>();
    if (sql == null) return occ;
    Matcher m = Pattern.compile(":(\\w+)").matcher(sql);
    while (m.find()) occ.add(m.group(1)); // names without leading ':'
    return occ;
  }

  public static List<String> uniqueInOrder(List<String> occ) {
    LinkedHashSet<String> set = new LinkedHashSet<>(occ);
    return new ArrayList<>(set);
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  public  class ParamTableModel extends AbstractTableModel {
    private final String[] cols = {"Name", "Type", "Default"};
    private final List<McpServerInputForm.ParamRow> rows = new ArrayList<>();

    public List<McpServerInputForm.ParamRow> getRows() {return rows;}

    @Override
    public int getRowCount() {
      return rows.size();
    }

    @Override
    public int getColumnCount() {
      return cols.length;
    }

    @Override
    public String getColumnName(int c) {
      return cols[c];
    }

    @Override
    public Class<?> getColumnClass(int c) {
      return c == 1 ? McpServerInputForm.ParamType.class : String.class;
    }

    @Override
    public boolean isCellEditable(int r, int c) {
      return c == 1 || c == 2;
    }

    @Override
    public Object getValueAt(int r, int c) {
      McpServerInputForm.ParamRow row = rows.get(r);
      switch (c) {
        case 0:
          return row.name;
        case 1:
          return row.type;
        case 2:
          return row.defaultValue;
        default:
          return null;
      }
    }

    @Override
    public void setValueAt(Object v, int r, int c) {
      McpServerInputForm.ParamRow row = rows.get(r);
      if (c == 1) {
        row.type = (v instanceof McpServerInputForm.ParamType) ? (McpServerInputForm.ParamType) v : McpServerInputForm.ParamType.String;
      } else if (c == 2) {
        row.defaultValue = v == null ? "" : v.toString();
      }
      fireTableRowsUpdated(r, r);
    }

    void removeRow(int idx) {
      rows.remove(idx);
      fireTableRowsDeleted(idx, idx);
    }
  }

  private static final class ParamHelpDialog extends DialogWrapper {
    private final String exampleSql;
    private final Consumer<String> onInsert;

    ParamHelpDialog(String exampleSql, Consumer<String> onInsert) {
      super(true); // use current window
      this.exampleSql = exampleSql;
      this.onInsert = onInsert;
      setTitle("Named parameters: quick guide");
      setOKButtonText("Close");
      init();
    }

    @Override
    protected JComponent createCenterPanel() {
      JPanel panel = new JPanel(new BorderLayout(8, 8));

      JLabel info = new JLabel(
              "<html>" +
                      "<b>Use <code>:param</code> placeholders in your SQL.</b>" +
                      "<ul style='margin-top:4; margin-bottom:4;'>" +
                      "<li>Write <code>:param</code> (e.g., <code>WHERE dept_id = :dept</code>).</li>" +
                      "<li>We detect names and list them in the Parameters table.</li>" +
                      "<li>At build-time they become JDBC <code>?</code> in left-to-right order.</li>" +
                      "<li>Repeated names bind the <i>same</i> value to each occurrence.</li>" +
                      "</ul>" +
                      "</html>"
      );
      panel.add(info, BorderLayout.NORTH);

      JTextArea example = new JTextArea(exampleSql);
      example.setEditable(false);
      example.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
      example.setLineWrap(false);
      JScrollPane sc = new JBScrollPane(example);
      sc.setBorder(BorderFactory.createTitledBorder("Example SQL"));
      panel.add(sc, BorderLayout.CENTER);

      JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      JButton copy = new JButton("Copy example");
      copy.addActionListener(e -> CopyPasteManager.getInstance()
              .setContents(new StringSelection(exampleSql)));
      JButton insert = new JButton("Insert into Query");
      insert.addActionListener(e -> {
        if (onInsert != null) onInsert.accept(exampleSql);
        close(OK_EXIT_CODE);
      });
      buttons.add(copy);
      buttons.add(insert);
      panel.add(buttons, BorderLayout.SOUTH);

      return panel;
    }
  }

  public ToolDefinitionModel getToolDefinitionModel() {
    ToolDefinitionModel model = new ToolDefinitionModel( paramsModel);
    model.setName(toolName.getText());
    model.setDescription(toolDescriptionTextArea.getText());
    model.setSql(queryTextArea.getText());
    return model;
  }
}
