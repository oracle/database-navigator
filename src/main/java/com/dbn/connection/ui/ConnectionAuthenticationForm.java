package com.dbn.connection.ui;

import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.connection.AuthenticationType;
import com.dbn.connection.ConnectionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import static com.dbn.common.text.TextContent.plain;


/**
 * Wrapper for the {@link ConnectionAuthenticationFieldsForm} to be used in connection authentication popups
 */
public class ConnectionAuthenticationForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel fieldsPanel;

    private final ConnectionAuthenticationFieldsForm fieldsForm = new ConnectionAuthenticationFieldsForm(this);

    ConnectionAuthenticationForm(@NotNull ConnectionAuthenticationDialog parentComponent, @Nullable ConnectionHandler connection) {
        super(parentComponent);

        initHeaders(connection);
        initFields(parentComponent, connection);
    }

    private void initFields(@NotNull ConnectionAuthenticationDialog parentComponent, @Nullable ConnectionHandler connection) {
        AuthenticationInfo authenticationInfo = parentComponent.getAuthenticationInfo();
        fieldsPanel.add(fieldsForm.getComponent(), BorderLayout.CENTER);
        AuthenticationType[] authTypes = connection == null ?
                AuthenticationType.values() :
                connection.getDatabaseType().getAuthTypes();

        fieldsForm.setAuthenticationTypes(authTypes);

        fieldsForm.resetFormChanges(authenticationInfo);
        fieldsForm.addChangeListeners(() -> {
            fieldsForm.applyFormChanges(authenticationInfo);
            parentComponent.updateConnectButton();
        });
    }

    private void initHeaders(@Nullable ConnectionHandler connection) {
        TextContent hintText;
        if (connection != null) {
            DBNHeaderForm headerForm = new DBNHeaderForm(this, connection);
            headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

            int passwordExpiryTime = connection.getSettings().getDetailSettings().getCredentialExpiryMinutes();
            String expiryTimeText = passwordExpiryTime == 0 ? "0 - no expiry" :
                    passwordExpiryTime == 1 ? "1 minute" : passwordExpiryTime + " minutes";

            hintText = plain("The system needs your credentials to connect to this database. " +
                            "\nYou can configure how long the credentials stay active on idle connectivity " +
                            "in DBN Settings > Connection > Details (currently set to " + expiryTimeText + ")");

        } else {
            hintText = plain("The system needs your credentials to connect to this database.");
        }
        DBNHintForm hintForm = new DBNHintForm(this, hintText, null, true);
        hintPanel.add(hintForm.getComponent(), BorderLayout.CENTER);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return fieldsForm.getPreferredFocusedComponent();
    }
}
