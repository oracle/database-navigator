package com.dbn.vector.ui.destination;

import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.object.DBSchema;
import com.dbn.object.common.ui.DBObjectSelector;
import com.dbn.vector.model.request.EmbeddingDestinationConfig;
import com.dbn.vector.ui.VectorToolboxFormBase;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.Set;
import java.util.regex.Pattern;

import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.object.type.DBObjectType.SCHEMA;

public class EmbeddingDestinationNewTableForm extends VectorToolboxFormBase {
  private JPanel mainPanel;
  private JTextField tableNameTextField;
  private JTextField vectorColumnTextField;
  private JTextField textColumnTextField;
  private JLabel tableNameLabel;
  private JLabel textColumnLabel;
  private JLabel keyColumnNameLabel;
  private JLabel vectorColumnLabel;
  private JTextField keyColumnTextField;
  private JLabel metadataColumnLabel;
  private JTextField metadataColumnTextField;
  private JLabel schemaLabel;
  private DBObjectSelector<DBSchema> schemaComboBox;
  // Oracle unquoted identifier rules (safe baseline)
  private static final int ORACLE_ID_MAX_LEN = 30;
  private static final Pattern ORACLE_UNQUOTED_ID =
          Pattern.compile("^[A-Za-z][A-Za-z0-9_$#]{0,29}$");

  private static final Set<String> ORACLE_RESERVED = Set.of(
          "SELECT","FROM","WHERE","GROUP","ORDER","BY","TABLE","INDEX","VIEW","TRIGGER",
          "SEQUENCE","USER","SESSION","NUMBER","DATE","INSERT","UPDATE","DELETE","CREATE",
          "ALTER","DROP","GRANT","REVOKE","AND","OR","NOT","NULL"
  );


  public EmbeddingDestinationNewTableForm(@NotNull VectorToolboxFormBase parent) {
    super(parent);
    initComboBoxes();
  }

  private void initComboBoxes() {

  }

  @Override
  public DBSchema getSelectedSchema() {
    return ComboBoxes.getSelection(schemaComboBox);
  }

  public String getTableName() {
    return getText(tableNameTextField);
  }


  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  public EmbeddingDestinationConfig getConfig() {
    return getEmbeddingRequest().getDestinationConfig();
  }

  @Override
  public void resetFormChanges() {
    EmbeddingDestinationConfig config = getConfig();

    tableNameTextField.setText(config.getTableName());
    keyColumnTextField.setText(config.getKeyColumnName());
    textColumnTextField.setText(config.getTextColumnName());
    vectorColumnTextField.setText(config.getEmbeddingColumnName());
    metadataColumnTextField.setText(config.getMetadataColumnName());

    schemaComboBox
            .initialize(this, SCHEMA)
            .withConnectionContext(() -> getConnection())
            .withValueLoader(() -> loadSchemas())
            .withValuePreselector(() -> config.getSchemaName())
            .triggerLoad();
  }

  @Override
  public void applyFormChanges() {
    EmbeddingDestinationConfig config = getConfig();
    config.setSchemaName(getSelectedObjectName(schemaComboBox, config.getSchemaName()));
    config.setTableName(getText(tableNameTextField));
    config.setEmbeddingColumnName(getText(vectorColumnTextField));
    config.setTextColumnName(getText(textColumnTextField));
    config.setKeyColumnName(getText(keyColumnTextField));
    config.setMetadataColumnName(getText(metadataColumnTextField));
  }

  @Override
  protected void initValidation() {
    addTextValidation(tableNameTextField,this::isValidOracleUnquotedIdentifier,"Enter a valid Oracle table name (1–30 chars, start with a letter; letters/digits/_/$/#; avoid reserved words).");
  }

  private static boolean isReservedWord(String s) {
    return s != null && ORACLE_RESERVED.contains(s.toUpperCase());
  }

  private boolean isValidOracleUnquotedIdentifier(String s) {
    if (s == null) return false;
    String name = s.trim();
    if (name.isEmpty()) return false;
    if (name.length() > ORACLE_ID_MAX_LEN) return false;
    if (!ORACLE_UNQUOTED_ID.matcher(name).matches()) return false;
    if (isReservedWord(name)) return false;
    return true;
  }

  @Override
  protected void initFieldAlignment() {
    FieldAlignerData alignerData = getFieldAlignerData();
    alignerData.registerFieldGroup(schemaLabel, schemaComboBox);
    alignerData.registerFieldGroup(tableNameLabel, tableNameTextField);
    alignerData.registerFieldGroup(textColumnLabel, textColumnTextField);
    alignerData.registerFieldGroup(keyColumnNameLabel, keyColumnTextField);
    alignerData.registerFieldGroup(vectorColumnLabel, vectorColumnTextField);
    alignerData.registerFieldGroup(metadataColumnLabel, metadataColumnTextField);
  }
}
