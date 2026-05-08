package com.dbn.oci.ui;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.oci.util.WalletPathValidator;
import com.dbn.oci.util.WalletPathValidator.WalletValidationResult;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBPasswordField;
import com.oracle.oci.intellij.ui.common.AutonomousDatabaseConstants;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.Arrays;
import java.util.Objects;

import static com.dbn.common.util.FileChoosers.addSingleFileChooser;


public class OciConnectionInputForm extends DBNFormBase {
  public static final String AUTHENTICATION_TYPE_MTLS = "Mutual TLS (Wallet Required)";
  public static final String AUTHENTICATION_TYPE_TLS = "TLS (Walletless)";
  public static final String WALLET_DEFAULT_LOCATION = System.getProperty("user.home") + "/.oci_toolkit/wallets";

  private final String walletDefaultPath;

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

  @Getter
  private boolean walletDownload; // indicator that wallet download is required
  String walletTooltip = "Please provide either an empty folder or a valid wallet location containing only the wallet files,<br> with no additional files or directories.";

  public OciConnectionInputForm(@Nullable Disposable parent, boolean isMtlsRequired, String parentCompartment) {
    super(parent);
    this.walletDefaultPath = WALLET_DEFAULT_LOCATION+"/"+parentCompartment;
    initComboBox();
    setListeners();
    initForm(isMtlsRequired,this.walletDefaultPath);
    initValidation();
  }

  private void initComboBox() {
    authenticationTypeComboBox.addItem(AUTHENTICATION_TYPE_MTLS);
    authenticationTypeComboBox.addItem(AUTHENTICATION_TYPE_TLS);
    walletTypeComboBox.addItem(AutonomousDatabaseConstants.INSTANCE_WALLET);
    walletTypeComboBox.addItem(AutonomousDatabaseConstants.REGIONAL_WALLET);

    addSingleFileChooser(
            getProject(),
            walletLocationField,
            "Select Wallet",
            "Select an Oracle database wallet folder");
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

  protected void initValidation() {
    // Validator for the confirm password field
    addTextValidation(passwordTextField, this::validatePassword,"Password must be 8-60 characters long and include at least one letter and one number.");

    // Validator for the confirm password field
    addTextValidation(passwordConfirmTextField, this::validateConfirmPassword,"Confirm Password does not match.");

    addTextValidation(walletLocationField.getTextField(),this::validateWalletPath,"<html><b>Error:</b> Invalid location:<br>"
            +walletTooltip+"</html>");
  }

  public boolean validatePassword(String passwordText) {
    if (isSpecifyPassword()) {
      // Check length
      if (passwordText.length() < 8 || passwordText.length() > 60) {
        return false;
      }

      // Check for alphabetic character
      if (!passwordText.matches(".*[a-zA-Z].*")) {
        return false;
      }

      // Check for numeric character
      if (!passwordText.matches(".*\\d.*")) {
        return false;
      }
    }

    return true; // Validation passed
  }

  public boolean validateConfirmPassword(String confirmPasswordText) {
    if (isSpecifyPassword()) {
      String password = new String(passwordTextField.getPassword());

      if (!password.equals(confirmPasswordText)) {
        return false;
      }
    }
    return true;
  }

  public boolean validateWalletPath(String walletPath) {
    if (isMTLS()) {
      walletPath = walletPath.trim(); // todo are we sure we want to trim here?
      WalletValidationResult validationResult = WalletPathValidator.validateWalletLocation(walletPath, walletDefaultPath);

      switch (validationResult) {
        case VALID_EMPTY_LOCATION:
          isSpecifyPasswordCheckbox.setVisible(true);
          walletDownload = true;
          return true;
        case VALID_EXISTING_WALLET:
          walletDownload = false;
          isSpecifyPasswordCheckbox.setVisible(false);
          return true;
        case INVALID_LOCATION:
          walletDownload = false;
          isSpecifyPasswordCheckbox.setVisible(false);
          return false;
      }
    }
    return true;
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
}
