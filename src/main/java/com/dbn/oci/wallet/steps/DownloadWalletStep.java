package com.dbn.oci.wallet.steps;

import com.dbn.oci.actions.CreateConnectionDBNAction;
import com.dbn.oci.wallet.ExpressConnectionWizardModel;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.util.Optional;

public class DownloadWalletStep extends WizardStep<ExpressConnectionWizardModel> implements Disposable {

  private static final FileChooserDescriptor FILE_CHOOSER_DESCRIPTOR = new FileChooserDescriptor(false, true, false, false, false, false)
          .withTitle("Select Wallet")
          .withDescription("Select a valid Oracle Database wallet");

  private JPanel mainPanel;
  private TextFieldWithBrowseButton fileLocationTextField;
  private JBPasswordField passwordField;
  private JBPasswordField confirmPasswordField;

  private boolean isFormValid = false;

  public DownloadWalletStep(Project project) {
    initializeUIComponents(project);
    setupValidation();
  }

  /**
   * Initializes UI components and their respective listeners.
   */
  private void initializeUIComponents(Project project) {
    fileLocationTextField.addBrowseFolderListener(
            null, null, project, FILE_CHOOSER_DESCRIPTOR
    );
  }




  /**
   * Validates password fields and returns a ValidationInfo object if validation fails.
   *
   * @return ValidationInfo if validation fails, null otherwise
   */
  private void setupValidation() {
    // Validator for the password field
    new ComponentValidator(this)
            .withValidator(() -> validatePassword(passwordField))
            .installOn(passwordField);

    // Validator for the confirm password field
    new ComponentValidator(this)
            .withValidator(() -> validateConfirmPassword(confirmPasswordField))
            .installOn(confirmPasswordField);

    addDocumentListenersToFields();
  }

  private ValidationInfo validatePassword(JPasswordField field) {
    String password = new String(field.getPassword());

    // Check length
    if (password.length() < 8 || password.length() > 60) {
      return new ValidationInfo("Password must be 8 to 60 characters.", field);
    }

    // Check for alphabetic character
    if (!password.matches(".*[a-zA-Z].*")) {
      return new ValidationInfo("Password must contain at least 1 alphabetic character.", field);
    }

    // Check for numeric character
    if (!password.matches(".*\\d.*")) {
      return new ValidationInfo("Password must contain at least 1 numeric character.", field);
    }

    return null; // Validation passed
  }

  private ValidationInfo validateConfirmPassword(JPasswordField field) {
    String password = new String(passwordField.getPassword());
    String confirmPassword = new String(confirmPasswordField.getPassword());

    if (!password.equals(confirmPassword)) {
      isFormValid = false;
      return new ValidationInfo("Passwords do not match.", field);
    }
    isFormValid = true;
    return null;
  }

  private void addDocumentListenersToFields() {
    DocumentAdapter listener = new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        // Revalidate both fields on input change
        ComponentValidator.getInstance(passwordField).ifPresent(ComponentValidator::revalidate);
        ComponentValidator.getInstance(confirmPasswordField).ifPresent(ComponentValidator::revalidate);
      }
    };

    passwordField.getDocument().addDocumentListener(listener);
    confirmPasswordField.getDocument().addDocumentListener(listener);
  }
  @Override
  public JComponent prepare(WizardNavigationState wizardNavigationState) {
    fileLocationTextField.setText(CreateConnectionDBNAction.WALLET_DEFAULT_LOCATION);
    return mainPanel;
  }

  @Override
  public WizardStep<ExpressConnectionWizardModel> onNext(ExpressConnectionWizardModel model) {
    // Trigger validation before moving to the next step
    ComponentValidator.getInstance(passwordField).ifPresent(ComponentValidator::revalidate);

    if (!isFormValid) {
      return this; // Stay on the current step if validation fails
    }

    // Proceed to the next step
    Optional<ConnectionDownloadWalletProgressStep> nextStep = model.getMySteps().stream()
            .filter(step -> step instanceof ConnectionDownloadWalletProgressStep)
            .map(ConnectionDownloadWalletProgressStep.class::cast)
            .findFirst();

    nextStep.ifPresent(step -> step.startDownload(
            model.getConnectionSettings(),
            new String(passwordField.getPassword()),
            fileLocationTextField.getText()
    ));

    return super.onNext(model);
  }

  @Override
  public void dispose() {
    // Perform cleanup if necessary
  }
}