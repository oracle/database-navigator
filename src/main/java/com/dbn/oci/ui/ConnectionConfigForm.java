package com.dbn.oci.ui;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.TextFields;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBPasswordField;
import com.oracle.oci.intellij.ui.common.AutonomousDatabaseConstants;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.util.Arrays;
import java.util.Objects;


public class ConnectionConfigForm extends DBNFormBase {
  public static String AUTHENTICATION_TYPE_MTLS = "Mutual TLS (Wallet Required)";
  public static String AUTHENTICATION_TYPE_TLS = "TLS (Walletless)";
  public static String WALLET_DEFAULT_LOCATION = System.getProperty("user.home") + "/.oci_toolkit/wallets";
  private final String walletDefaultPath;

  public enum WalletPathType{
    EMPTY_FOLDER,
    EXISTING_WALLET,
  }
  private static final FileChooserDescriptor FILE_CHOOSER_DESCRIPTOR = new FileChooserDescriptor(false, true, false, false, false, false)
          .withTitle("Select Wallet")
          .withDescription("Select a valid Oracle Database wallet");


  private JPanel mainPanel;
  private ComboBox authenticationTypeComboBox;
  private ComboBox<String> walletTypeComboBox;
  private JCheckBox isSpecifyPasswordCheckbox;
  private JBPasswordField passwordTextField;
  private JBPasswordField passwordConfirmTextField;
  private JLabel authenticationLabel;
  private JLabel walletTypeLabel;
  private JLabel walletLocationLabel;
  private JLabel passwordLabel;
  private JLabel confirmPasswordLabel;
  private TextFieldWithBrowseButton walletLocationField;
  private JLabel walletLocationHelper;
  private boolean isDirtyWalletPath = false ;
  @Getter
  WalletPathType walletPathType ;
  String walletTooltip = "Please provide either an empty folder or a valid wallet location containing only the wallet files,<br> with no additional files or directories.";

  public ConnectionConfigForm(@Nullable Disposable parent, boolean isMtlsRequired, String parentCompartment) {
    super(parent);
    this.walletDefaultPath = WALLET_DEFAULT_LOCATION+"/"+parentCompartment;
    initComboBox();
    setListeners();
    initForm(isMtlsRequired,this.walletDefaultPath);
    setupValidation();

  }

  private void initComboBox() {
    authenticationTypeComboBox.addItem(AUTHENTICATION_TYPE_MTLS);
    authenticationTypeComboBox.addItem(AUTHENTICATION_TYPE_TLS);
    walletTypeComboBox.addItem(AutonomousDatabaseConstants.INSTANCE_WALLET);
    walletTypeComboBox.addItem(AutonomousDatabaseConstants.REGIONAL_WALLET);

    walletLocationField.addBrowseFolderListener(
            null, null, getProject(), FILE_CHOOSER_DESCRIPTOR
    );

  }

  private void setListeners() {
    authenticationTypeComboBox.addActionListener(e -> {
      if (AUTHENTICATION_TYPE_MTLS.equals(authenticationTypeComboBox.getSelectedItem())) {
        setAuthenticationTypeToMTLS();
      }else {
        setAuthenticationTypeToTLS();
      }
    });

    isSpecifyPasswordCheckbox.addActionListener((e)->{
      passwordTextField.setVisible(isSpecifyPasswordCheckbox.isSelected());
      passwordConfirmTextField.setVisible(isSpecifyPasswordCheckbox.isSelected());
      passwordLabel.setVisible(isSpecifyPasswordCheckbox.isSelected());
      confirmPasswordLabel.setVisible(isSpecifyPasswordCheckbox.isSelected());
    });
  }

  private void initForm(boolean isMtlsRequired, String parentCompartment) {
    walletLocationField.setText(walletDefaultPath);
    walletLocationField.setToolTipText(walletTooltip);
    isDirtyWalletPath = !validateWalletPath();
    passwordTextField.setVisible(false);
    passwordLabel.setVisible(false);
    passwordConfirmTextField.setVisible(false);
    confirmPasswordLabel.setVisible(false);

    if (isMtlsRequired) {
      authenticationTypeComboBox.setEnabled(false);
      authenticationLabel.setEnabled(false);
      authenticationTypeComboBox.setSelectedItem(AUTHENTICATION_TYPE_MTLS);
      setAuthenticationTypeToMTLS();
    }else {
      authenticationTypeComboBox.setSelectedItem(AUTHENTICATION_TYPE_TLS);
      setAuthenticationTypeToTLS();
    }



  }

  private void setAuthenticationTypeToMTLS() {
    walletTypeComboBox.setVisible(true);
    walletTypeLabel.setVisible(true);
    walletLocationLabel.setVisible(true);
    walletLocationHelper.setVisible(true);
    walletLocationField.setVisible(true);
    validateWalletPath();

  }

  private void setAuthenticationTypeToTLS() {
    walletTypeComboBox.setVisible(false);
    walletTypeLabel.setVisible(false);
    walletLocationLabel.setVisible(false);
    walletLocationHelper.setVisible(false);
    walletLocationField.setVisible(false);
    isSpecifyPasswordCheckbox.setVisible(false);
    authenticationLabel.setLabelFor(authenticationTypeComboBox);
  }

  private void setupValidation() {
    // Validator for the password field
    new ComponentValidator(this)
            .withValidator(this::validatePassword)
            .installOn(passwordTextField);

    // Validator for the confirm password field
    new ComponentValidator(this)
            .withValidator(this::validateConfirmPassword)
            .installOn(passwordConfirmTextField);

    addDocumentListenersToFields();
  }

  public ValidationInfo validatePassword() {
    String password = new String(passwordTextField.getPassword());

    // Check length
    if (password.length() < 8 || password.length() > 60) {
      return new ValidationInfo("Password must be 8 to 60 characters.", passwordTextField);
    }

    // Check for alphabetic character
    if (!password.matches(".*[a-zA-Z].*")) {
      return new ValidationInfo("Password must contain at least 1 alphabetic character.", passwordTextField);
    }

    // Check for numeric character
    if (!password.matches(".*\\d.*")) {
      return new ValidationInfo("Password must contain at least 1 numeric character.", passwordTextField);
    }

    return null; // Validation passed
  }

  public ValidationInfo validateConfirmPassword() {
    String password = new String(passwordTextField.getPassword());
    String confirmPassword = new String(passwordConfirmTextField.getPassword());

    if (!password.equals(confirmPassword)) {
      return new ValidationInfo("Passwords do not match.", passwordConfirmTextField);
    }
    return null;
  }

  private void addDocumentListenersToFields() {
    walletLocationField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {

      @Override
      protected void textChanged(@NotNull DocumentEvent documentEvent) {
          isDirtyWalletPath = !validateWalletPath();
      }
    });
    DocumentAdapter listener = new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        // Revalidate both fields on input change
        ComponentValidator.getInstance(passwordTextField).ifPresent(ComponentValidator::revalidate);
        ComponentValidator.getInstance(passwordConfirmTextField).ifPresent(ComponentValidator::revalidate);
      }
    };

    passwordTextField.getDocument().addDocumentListener(listener);
    passwordConfirmTextField.getDocument().addDocumentListener(listener);
  }

  public boolean validateWalletPath() {
    String walletPath = walletLocationField.getText().trim();
    WalletPathValidator.WalletValidationResult validationResult = WalletPathValidator.validateWalletLocation(walletPath,walletDefaultPath);

    switch (validationResult) {
      case VALID_EMPTY_LOCATION:
        TextFields.updateFieldError(walletLocationField.getTextField(),null);
        walletLocationField.getTextField().setToolTipText("A wallet will be downloaded in this folder");
        walletPathType = WalletPathType.EMPTY_FOLDER;
        isSpecifyPasswordCheckbox.setVisible(true);
        return true;
      case VALID_EXISTING_WALLET:

        TextFields.updateFieldError(walletLocationField.getTextField(),null);
        walletLocationField.getTextField().setToolTipText("A valid wallet already exists ");
        this.walletPathType = WalletPathType.EXISTING_WALLET;
        return true;
      case INVALID_LOCATION:

        TextFields.updateFieldError(walletLocationField.getTextField(),"<html><b>Error:</b> Invalid location:<br>"
                                                                            +walletTooltip+"</html>");
        isSpecifyPasswordCheckbox.setVisible(false);

        return false;
    }
    return false;
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }
  public boolean  isMTLS(){
    return Objects.equals( authenticationTypeComboBox.getSelectedItem(), AUTHENTICATION_TYPE_MTLS);
  }
  public String  getWalletLocation(){
    return walletLocationField.getText();
  }
  public String  getPassword(){
    return Arrays.toString(passwordTextField.getPassword());
  }

  public boolean isSpecifyPassword(){
    return isSpecifyPasswordCheckbox.isSelected();
  }

  public boolean isDirtyWalletPath() {
    return isDirtyWalletPath;
  }
}
