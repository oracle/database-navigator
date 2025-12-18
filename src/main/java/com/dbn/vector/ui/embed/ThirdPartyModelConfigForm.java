package com.dbn.vector.ui.embed;

import com.dbn.assistant.AssistantType;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.provider.AIProviderData;
import com.dbn.assistant.service.selectai.credential.ui.CredentialEditDialog;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.link.HyperLinkForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.DBCredential;
import com.dbn.object.DBSchema;
import com.dbn.object.event.ObjectChangeEvent;
import com.dbn.object.type.DBObjectType;
import com.dbn.vector.model.embed.ThirdPartyModelConfig;
import com.dbn.vector.ui.VectorToolboxFormBase;
import com.intellij.openapi.Disposable;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Set;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.ui.ValueSelectorOption.HIDE_DESCRIPTION;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.TextFields.getText;
import static java.util.Collections.emptyList;

public class ThirdPartyModelConfigForm extends VectorToolboxFormBase {
  private JPanel mainPanel;
  private JBTextField urlTextField;
  private JBTextField modelTextField;
  private JLabel providerLabel;
  private JLabel credentialLabel;
  private JLabel urlLabel;
  private JLabel modelLabel;
  private JLabel credentialSchemaLabel;
  private DBNComboBox<DBSchema> credentialSchemaComboBox;
  private DBNComboBox<DBCredential> credentialComboBox;
  private DBNComboBox<AIProvider> providerComboBox;
  private JButton addCredentialButton;
  private JPanel hyperLinkPanel;

  public ThirdPartyModelConfigForm(@Nullable Disposable parent, ConnectionHandler connection) {
    super(parent, connection);
    initComboBoxes();
    initCredentialAddButton();
    initDocumentationLinkPanel();
  }
  private void initDocumentationLinkPanel() {
    HyperLinkForm hyperLinkForm = HyperLinkForm.create(
            "",
            "Supported Third-Party Providers",
            "https://docs.oracle.com/en/database/oracle/oracle-database/26/vecse/supported-third-party-provider-operations-and-endpoints.html");

    hyperLinkPanel.add(hyperLinkForm.getComponent(), BorderLayout.WEST);
  }
  private void initComboBoxes() {
    List<AIProvider> providers = AIProviderData.getProviders(AssistantType.VECTOR_AI);
    ComboBoxes.initComboBox(providerComboBox, providers);

    credentialComboBox.set(HIDE_DESCRIPTION, true);
    credentialSchemaComboBox.set(HIDE_DESCRIPTION, true);

    updateFieldAvailability();
  }

  private void initCredentialAddButton() {
    addCredentialButton.setIcon(Icons.ACTION_ADD);
    addCredentialButton.setText(null);

    addCredentialButton.addActionListener(e -> Dialogs.show(() ->
            new CredentialEditDialog(getConnection(), null, null, Set.of())));

    ObjectChangeEvent.subscribe(this,
            getConnection(),
            DBObjectType.CREDENTIAL,
            () -> credentialComboBox.reloadValues());
  }

  @Override
  protected void initFieldAvailability() {
    DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
    fieldAdapter.initFieldsAvailability(() -> isValid(getSelectedSchema()), array(credentialComboBox));
    fieldAdapter.initFieldsVisibility(() -> isValid(getSelectedSchema()), array(addCredentialButton));
  }

  @Override
  protected void initEventListeners() {
    onSelectionChange(credentialSchemaComboBox, s -> populateCredentials());
  }

  @Override
  protected void initValidation() {
    addSelectionValidation(credentialSchemaComboBox, "Please select a credential schema");
    addSelectionValidation(credentialComboBox, "Please select or create a credential");
    addSelectionValidation(providerComboBox, "Please specify the embedding model provider");
    addTextValidation(urlTextField, t -> Strings.isNotEmpty(t), "Please specify the embedding model URL");
    addTextValidation(modelTextField, t -> Strings.isNotEmpty(t), "Please specify the embedding model name");
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  public String getProviderApiName() {
    AIProvider provider = getProvider();
    return provider == null ? null : provider.getApiName();
  }

  public @Nullable AIProvider getProvider() {
    return getSelection(providerComboBox);
  }

  public String getUrl() {
    return urlTextField.getText();
  }

  public String getModelName() {
    return modelTextField.getText();
  }

  public ThirdPartyModelConfig getConfig() {
    return getEmbeddingRequest().getEmbedConfig().getThirdPartyModelConfig();
  }

  @Override
  public DBSchema getSelectedSchema() {
    return getSelection(credentialSchemaComboBox);
  }

  private void populateCredentials() {
    updateFieldAvailability();
    credentialComboBox.reloadValues();
  }

  private List<DBCredential> loadCredentials() {
    DBSchema schema = getSelectedSchema();
    if (schema == null) return emptyList();

    return schema.getCredentials();
  }

  @Override
  public void resetFormChanges() {
    ThirdPartyModelConfig config = getConfig();
    setSelection(providerComboBox, getProvider(config.getProvider()));
    modelTextField.setText(config.getModelName());
    urlTextField.setText(config.getEndpointUrl());
    credentialSchemaComboBox.initialize(() -> loadSchemas(), s -> matchesObjectName(s, config.getCredentialSchemaName()));
    credentialComboBox.initialize(() -> loadCredentials(), m -> matchesObjectName(m, config.getCredentialName()));
  }

  private static @Nullable AIProvider getProvider(String apiName) {
    return AIProviderData.getProvider(AssistantType.VECTOR_AI, p -> p.getApiName().equals(apiName));
  }

  @Override
  public void applyFormChanges() {
    ThirdPartyModelConfig config = getConfig();
    config.setProvider(getProviderApiName());
    config.setModelName(getText(modelTextField));
    config.setEndpointUrl(getText(urlTextField));
    config.setCredentialSchemaName(getSelectedObjectName(credentialSchemaComboBox, config.getCredentialSchemaName()));
    config.setCredentialName(getSelectedObjectName(credentialComboBox, config.getCredentialName()));
  }

  @Override
  protected void initFieldAlignment() {
    FieldAlignerData alignerData = getFieldAlignerData();
    alignerData.registerFieldGroup(providerLabel, providerComboBox);
    alignerData.registerFieldGroup(modelLabel, modelTextField);
    alignerData.registerFieldGroup(urlLabel, urlTextField);
    alignerData.registerFieldGroup(credentialSchemaLabel, credentialSchemaComboBox);
    alignerData.registerFieldGroup(credentialLabel, credentialComboBox);
  }
}
