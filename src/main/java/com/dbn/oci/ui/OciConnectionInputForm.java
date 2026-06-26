package com.dbn.oci.ui;

import com.dbn.common.ui.Presentable;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.oci.util.WalletPathValidator;
import com.dbn.oci.util.WalletPathValidator.WalletValidationResult;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBPasswordField;
import com.oracle.oci.intellij.ui.common.AutonomousDatabaseConstants;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.Arrays;

import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.PasswordFields.testPassword;
import static com.dbn.common.util.FileChoosers.addSingleFileChooser;
import static com.dbn.common.util.Passwords.verifyPassword;
import static com.dbn.nls.NlsResources.txt;


public class OciConnectionInputForm extends DBNFormBase {
    public static final @NonNls String WALLET_DEFAULT_LOCATION = System.getProperty("user.home") + "/.oci_toolkit/wallets";

    private final String walletDefaultPath;

    private JPanel mainPanel;
    private ComboBox<AuthenticationType> authenticationTypeComboBox;
    private ComboBox<String> walletTypeComboBox;
    private JCheckBox specifyPasswordCheckbox;
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

    public OciConnectionInputForm(@Nullable Disposable parent, boolean isMtlsRequired, String parentCompartment) {
        super(parent);
        this.walletDefaultPath = WALLET_DEFAULT_LOCATION + "/" + parentCompartment;
        initComboBoxes();
        setListeners();
        initForm(isMtlsRequired, this.walletDefaultPath);
        initValidation();
    }

    private void initComboBoxes() {
        initComboBox(authenticationTypeComboBox,
                AuthenticationType.MTLS,
                AuthenticationType.TLS);

        initComboBox(walletTypeComboBox,
                AutonomousDatabaseConstants.INSTANCE_WALLET,
                AutonomousDatabaseConstants.REGIONAL_WALLET);

        addSingleFileChooser(
                getProject(),
                walletLocationField,
                txt("cfg.oci.title.SelectWallet"),
                txt("cfg.oci.text.SelectWallet"));
    }

    private void setListeners() {
        authenticationTypeComboBox.addActionListener(e -> updateFieldAvailability());
        specifyPasswordCheckbox.addActionListener(e -> updateFieldAvailability());
    }

    private void initForm(boolean isMtlsRequired, String parentCompartment) {
        walletLocationField.setText(walletDefaultPath);
        walletLocationField.setToolTipText(txt("cfg.oci.tooltip.WalletLocation"));

        if (isMtlsRequired) {
            authenticationTypeComboBox.setEnabled(false);
            authenticationLabel.setEnabled(false);
            setSelection(authenticationTypeComboBox, AuthenticationType.MTLS);
        } else {
            setSelection(authenticationTypeComboBox, AuthenticationType.TLS);
        }
        authenticationLabel.setLabelFor(authenticationTypeComboBox);
    }

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsVisibility(() -> isMtls(), array(
                walletTypeComboBox,
                walletTypeLabel,
                walletLocationLabel,
                walletLocationHelper,
                walletLocationField));

        fieldAdapter.initFieldsVisibility(() -> isWalletDownloadAvailable(), array(
                specifyPasswordCheckbox));

        fieldAdapter.initFieldsVisibility(() -> isPasswordInputVisible(), array(
                passwordTextField,
                passwordConfirmTextField,
                passwordLabel,
                confirmPasswordLabel));
    }

    protected void initValidation() {
        addPasswordValidation(passwordTextField, p -> validatePassword(p), txt("cfg.oci.error.WalletPasswordInvalid"));
        addPasswordValidation(passwordConfirmTextField, p -> validateConfirmPassword(p), txt("cfg.oci.error.WalletPasswordMismatch"));
        addTextValidation(walletLocationField.getTextField(), p -> validateWalletPath(p), txt("cfg.oci.error.WalletLocationInvalid", txt("cfg.oci.tooltip.WalletLocation")));
    }

    public boolean validatePassword(char[] password) {
        if (!isSpecifyPassword()) return true;
        return verifyPassword(password, 8, 60, true, true, false);
    }

    public boolean validateConfirmPassword(char[] confirmPassword) {
        if (!isSpecifyPassword()) return true;
        return testPassword(passwordTextField, password -> Arrays.equals(password, confirmPassword));
    }

    public boolean validateWalletPath(String walletPath) {
        if (!isMtls()) return true;

        walletPath = walletPath.trim(); // todo are we sure we want to trim here?
        WalletValidationResult validationResult = WalletPathValidator.validateWalletLocation(walletPath, walletDefaultPath);

        switch (validationResult) {
            case VALID_EMPTY_LOCATION:
                walletDownload = true;
                updateFieldAvailability();
                return true;
            case VALID_EXISTING_WALLET:
                walletDownload = false;
                updateFieldAvailability();
                return true;
            case INVALID_LOCATION:
                walletDownload = false;
                updateFieldAvailability();
                return false;
        }
        return true;
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public boolean isMtls() {
        return getSelection(authenticationTypeComboBox) == AuthenticationType.MTLS;
    }

    public String getWalletLocation() {
        return walletLocationField.getText();
    }

    public char[] getPassword() {
        return passwordTextField.getPassword();
    }

    public boolean isSpecifyPassword() {
        return specifyPasswordCheckbox.isSelected();
    }

    private boolean isWalletDownloadAvailable() {
        return isMtls() && isWalletDownload();
    }

    private boolean isPasswordInputVisible() {
        return isWalletDownloadAvailable() && isSpecifyPassword();
    }

    @Getter
    private enum AuthenticationType implements Presentable {
        MTLS(txt("cfg.oci.const.AuthenticationType_MTLS")),
        TLS(txt("cfg.oci.const.AuthenticationType_TLS"));

        private final String name;

        AuthenticationType(String name) {
            this.name = name;
        }
    }
}
