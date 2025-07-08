package com.dbn.vector.ui;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.panel.DBNCollapsiblePanel;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;

import javax.swing.*;

public class VectorAIForm extends DBNFormBase {
  private JPanel mainPanel;
  private JPanel dataPanel;
  private JPanel chunkConfigPanel;
  private JPanel embedConfigPanel;
  private JPanel saveDataPanel;
  private JComboBox comboBox1;
  private JComboBox comboBox2;
  private JComboBox comboBox3;
  private JComboBox comboBox4;
  private JSpinner spinner1;
  private JComboBox comboBox5;
  private JSpinner spinner2;
  private JButton chunkLaboButton;
  private JRadioButton inDatabaseModelRadioButton;
  private JRadioButton thirdpartyModelsRadioButton;
  private JComboBox comboBox6;
  private JCheckBox createNewOneCheckBox;
  private JTextField textField1;
  private JTextField textField2;
  private JComboBox comboBox7;
  private JSpinner spinner3;
  private JTextField textField3;
  private JTextField textField4;
  private JSpinner spinner4;
  private JButton applyButton;


  public VectorAIForm(ConnectionHandler connection) {
    super(connection, connection.getProject());

    System.out.println("VectorAIForm");
    chunkLaboButton.addActionListener(e -> {
      Dialogs.show(()->new ChunkEditorDialog(getProject(),"Chunk Editor",true,connection));
      System.out.println("hi");
      System.out.println("fjkdsfj");
    });

//    DBNCollapsiblePanel collapsiblePanel = new DBNCollapsiblePanel(this, dataPanel.getc, false);
//    collapsiblePanel.setExpanded(executionInput.isContextExpanded());
//    collapsiblePanel.addToggleListener(expanded -> executionInput.setContextExpanded(expanded));
//    mainPanel.add(collapsiblePanel.getComponent());
//    initComponents();
//    loadModels();
//    insertButton.addActionListener(this::onInsert);
//    chooseFilesButton.addActionListener(this::onChooseFiles);
  }



//  private void onInsert(ActionEvent e) {
//    int chunkSize = Integer.parseInt(chunkSizeField.getText());
//    String model   = (String)modelCombo.getSelectedItem();
//    List<String> files = java.util.List.of(fileList.getSelectedValuesList().toArray(new String[0]));
    // hand off to your VectorAIManager:
//    DatabaseVectorManager.getInstance(getProject())
//            .chunkEmbedAndInsert(getConnection(), files, chunkSize, model);
//  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }
}