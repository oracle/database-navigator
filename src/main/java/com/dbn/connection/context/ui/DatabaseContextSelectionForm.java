/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.connection.context.ui;

import com.dbn.common.message.MessageType;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.info.DBNCommentLabel;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.panel.DBNBanner;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.ConnectionType;
import com.dbn.connection.context.DatabaseContextSelection;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.object.DBSchema;
import com.dbn.object.common.ui.DBObjectSelector;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Action;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static com.dbn.common.ui.form.field.DBNFormFieldDisabler.setFormFieldEnabled;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.util.Lists.filter;
import static com.dbn.common.util.Lists.first;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.type.DBObjectType.SCHEMA;
import static java.util.Collections.emptyList;

/**
 * Form used to select and validate a connection, schema, and optional session.
 */
public class DatabaseContextSelectionForm extends DBNFormBase {
    private static final String CONTEXT_AVAILABILITY = "CONTEXT_AVAILABILITY";

    private JPanel mainPanel;
    private JPanel contextPanel;
    private JPanel verificationPanel;
    private JPanel actionsPanel;
    private DBNCommentLabel headerLabel;
    private JLabel connectionLabel;
    private JLabel schemaLabel;
    private JLabel sessionLabel;
    private DBNComboBox<ConnectionHandler> connectionSelector;
    private DBObjectSelector<DBSchema> schemaSelector;
    private DBNComboBox<DatabaseSession> sessionSelector;
    private JButton continueButton;

    private final DBNBanner verificationBanner;
    private final DatabaseContextSelectorInput input;
    private final boolean selectionInitialized;
    private final Action continueAction = createAction(
            txt("msg.shared.button.Continue"),
            () -> continueSelection());
    private Runnable contentChangeCallback;
    private Runnable continueCompletionCallback;

    public DatabaseContextSelectionForm(
            @NotNull Project project,
            @NotNull DatabaseContextSelectorInput input) {
        super(null, project);

        this.input = input;
        Objects.requireNonNull(input.getContinueCallback(), "continue callback must be configured");
        verificationBanner = new DBNBanner(MessageType.ERROR);
        verificationBanner.setVisible(false);
        verificationPanel.add(verificationBanner);
        continueButton.setAction(continueAction);
        continueAction.setEnabled(false);

        initHeader();
        initSelectors();
        selectionInitialized = true;
        whenFirstShown(this::updateValidationState);
    }

    private void initHeader() {
        headerLabel.setCopyable(false);
        headerLabel.setText(input.getHeaderContent().getText());
    }

    private void initSelectors() {
        DatabaseContextSelection initialSelection = input.getInitialSelection();
        boolean sessionEnabled = input.isSessionSelectionEnabled();
        Project project = ensureProject();

        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        List<ConnectionHandler> connections = connectionManager.getConnections();
        connectionSelector.setValues(connections);

        schemaSelector.initialize(this, SCHEMA);
        schemaSelector.withConnectionContext(() -> getSelectedConnection());
        schemaSelector
                .withValueLoader(() -> loadConnections())
                .withValueLoadConsumer(values -> updateValidationState());
        sessionSelector
                .withValueLoader(() -> loadSessions())
                .withValueLoadConsumer(values -> updateValidationState());

        onSelectionChange(connectionSelector, connection -> connectionSelected(connection));
        onSelectionChange(schemaSelector, schema -> updateValidationState());
        onSelectionChange(sessionSelector, session -> updateValidationState());

        if (initialSelection != null) {
            schemaSelector.withValuePreselector(() -> initialSelection.schemaId().getName());
            if (sessionEnabled && initialSelection.sessionId() != null) {
                sessionSelector.withValuePreselector(session -> session.getId().equals(initialSelection.sessionId()));
            }
            ConnectionHandler initialConnection = first(connections, c -> Objects.equals(c.getConnectionId(), initialSelection.connectionId()));
            if (initialConnection != null) {
                connectionSelector.setSelectedValue(initialConnection);
            }
        } else if (connections.size() == 1) {
            connectionSelector.setSelectedValue(connections.get(0));
        }

        sessionSelector.setVisible(sessionEnabled);
        sessionLabel.setVisible(sessionEnabled);
        updateContextFieldAvailability(getSelectedConnection());
        schemaSelector.triggerLoad();
        if (sessionEnabled) {
            sessionSelector.triggerLoad();
        }
    }

    private void connectionSelected(ConnectionHandler connection) {
        schemaSelector.reloadValues();
        if (input.isSessionSelectionEnabled()) {
            sessionSelector.reloadValues();
        }
        updateContextFieldAvailability(connection);
        updateValidationState();
    }

    private List<DatabaseSession> loadSessions() {
        ConnectionHandler connection = getSelectedConnection();
        return connection == null ? emptyList() : connection.getSessionBundle().getSessions(
                ConnectionType.MAIN,
                ConnectionType.POOL,
                ConnectionType.SESSION);
    }

    private List<DBSchema> loadConnections() {
        ConnectionHandler connection = getSelectedConnection();
        return connection == null ? emptyList() : filter(connection.getObjectBundle().getSchemas(), s -> !s.isSystemSchema());
    }

    private void updateContextFieldAvailability(@Nullable ConnectionHandler connection) {
        setFormFieldEnabled(schemaSelector, CONTEXT_AVAILABILITY, connection != null);
        setFormFieldEnabled(sessionSelector, CONTEXT_AVAILABILITY, connection != null);
    }

    private void updateValidationState() {
        if (!selectionInitialized || isDisposed()) return;

        DatabaseContextSelection selection = getSelection();
        String verificationError = selection == null ? null :
                input.getSelectionVerification().apply(selection);
        boolean valid = selection != null && verificationError == null;
        boolean showError = verificationError != null;

        verificationBanner.setMessage(verificationError);
        verificationBanner.setVisible(showError);
        verificationPanel.setVisible(showError);
        continueAction.setEnabled(valid);
        verificationPanel.revalidate();
        mainPanel.revalidate();
        mainPanel.repaint();
        notifyContentChanged();
    }

    @Nullable
    public DatabaseContextSelection getSelection() {
        ConnectionHandler connection = getSelectedConnection();
        DBSchema schema = schemaSelector.getSelectedValue();
        if (connection == null || schema == null) return null;

        DatabaseSession session = input.isSessionSelectionEnabled() ?
                sessionSelector.getSelectedValue() : null;
        return new DatabaseContextSelection(
                connection.getConnectionId(),
                schema.getSchemaId(),
                session == null ? null : session.getId());
    }

    public boolean isSelectionValid() {
        return getValidSelection() != null;
    }

    public boolean continueSelection() {
        DatabaseContextSelection selection = getValidSelection();
        if (selection == null) return false;

        Consumer<DatabaseContextSelection> continueCallback = input.getContinueCallback();
        if (continueCallback == null) return false;

        continueCallback.accept(selection);
        if (continueCompletionCallback != null) {
            continueCompletionCallback.run();
        }
        return true;
    }

    public void setContinueButtonVisible(boolean visible) {
        actionsPanel.setVisible(visible);
    }

    public void setContinueCompletionCallback(@Nullable Runnable continueCompletionCallback) {
        this.continueCompletionCallback = continueCompletionCallback;
    }

    public void setContentChangeCallback(@Nullable Runnable contentChangeCallback) {
        this.contentChangeCallback = contentChangeCallback;
    }

    private void notifyContentChanged() {
        if (contentChangeCallback != null) {
            contentChangeCallback.run();
        }
    }

    @Nullable
    private DatabaseContextSelection getValidSelection() {
        DatabaseContextSelection selection = getSelection();
        return selection != null && input.getSelectionVerification().apply(selection) == null ? selection : null;
    }

    @Nullable
    private ConnectionHandler getSelectedConnection() {
        return connectionSelector.getSelectedValue();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return connectionSelector;
    }
}
