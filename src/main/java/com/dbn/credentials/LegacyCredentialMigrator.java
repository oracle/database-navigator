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

package com.dbn.credentials;

import com.dbn.assistant.credential.AssistantCredential;
import com.dbn.common.routine.OperationQueue;
import com.dbn.common.state.StateAttributes;
import com.dbn.common.state.StateCategory;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.ReverseSshTunnelConfiguration;
import com.dbn.options.ProjectSettingsManager;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.credentialStore.OneTimeString;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.notification.NotificationCategory.CONNECTION;
import static com.dbn.common.notification.NotificationSupport.sendInfoNotification;
import static com.dbn.common.util.Lists.filter;
import static com.dbn.common.util.Messages.options;
import static com.dbn.credentials.DatabaseCredentialManager.createAttributes;
import static com.dbn.credentials.Secrets.initialize;
import static com.dbn.nls.NlsResources.txt;

/**
 * Temporary compatibility helper for restoring saved credentials from the legacy
 * PasswordSafe key format into the current project-scoped credential storage.
 * <p>
 * Migration is intentionally user supervised: legacy entries are detected and
 * copied only after the user confirms either the project-wide restore prompt or
 * the single-connection restore prompt. Legacy PasswordSafe entries are left in
 * place so users can still roll back to an older plugin version during the
 * transition period.
 * <p>
 * This class should disappear once legacy credential keys are no longer supported.
 */
public class LegacyCredentialMigrator {
    private static final StateCategory SECRET_STORAGE_MIGRATION = StateCategory.get("SECRET_STORAGE_MIGRATION");

    private final ProjectSettingsManager settingsManager;
    private final OperationQueue callbacks = new OperationQueue();

    public LegacyCredentialMigrator(ProjectSettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    public void promptCredentialRestore() {
        prompt(() -> initialize(), false);
    }

    public void ensureAuthenticationAvailable(
            ConnectionHandler connection,
            Runnable resolved,
            Runnable unresolved,
            Runnable cancel) {
        Background.run(() -> {
            reloadConnectionSecrets(connection);
            if (connection.isAuthenticationProvided()) {
                dispatch(resolved);
                return;
            }

            promptConnection(
                    connection,
                    () -> dispatch(() -> {
                        if (connection.isAuthenticationProvided()) {
                            resolved.run();
                        } else {
                            unresolved.run();
                        }
                    }),
                    () -> dispatch(unresolved),
                    () -> dispatch(cancel));
        });
    }

    private static void reloadConnectionSecrets(ConnectionHandler connection) {
        connection.getAuthenticationInfo().reloadSecrets();
        ConnectionSettings settings = connection.getSettings();
        settings.getSshTunnelSettings().reloadSecrets();
        settings.getDebuggerSettings().getReverseSshTunnelConfig().reloadSecrets();
    }

    private static void dispatch(Runnable runnable) {
        Dispatch.run(ModalityState.nonModal(), runnable);
    }

    private void prompt(Runnable callback, boolean requireMigration) {
        if (isMigrationComplete()) {
            callback.run();
            return;
        }

        if (!callbacks.enqueue(callback, requireMigration)) return;

        Background.run(() -> {
            Project project = settingsManager.getProject();
            List<Candidate> candidates = pending(candidates(settingsManager));
            if (candidates.isEmpty()) {
                setMigrationComplete();
                completeCallbacks();
                return;
            }

            int option = Messages.showAcknowledgementDialog(
                    project,
                    txt("msg.credentials.title.CredentialRestore"),
                    txt("msg.credentials.question.CredentialRestore", candidates.size(), preview(candidates)),
                    options(
                            txt("msg.credentials.button.RestoreCredentials"),
                            txt("msg.credentials.button.RestoreLater")),
                    0,
                    null);

            if (option == 0) {
                int migrated = migrate(candidates);
                sendInfoNotification(
                        project,
                        CONNECTION,
                        txt("ntf.credentials.info.CredentialRestoreComplete", migrated));
                setMigrationComplete();
            }
            completeCallbacks();
        });
    }

    private void promptConnection(
            ConnectionHandler connection,
            Runnable callback,
            Runnable noMigration,
            Runnable cancel) {
        if (isMigrationComplete()) {
            noMigration.run();
            return;
        }

        Background.run(() -> {
            List<Candidate> candidates = pending(candidates(connection.getSettings()));
            if (candidates.isEmpty()) {
                noMigration.run();
                return;
            }

            int option = Messages.showConfirmationDialog(
                    connection.getProject(),
                    txt("msg.credentials.title.CredentialRestore"),
                    txt("msg.credentials.question.ConnectionCredentialRestore", connection.getName()),
                    options(
                            txt("msg.credentials.button.RestoreAndConnect"),
                            txt("msg.credentials.button.RestoreAllCredentials"),
                            txt("msg.shared.button.Cancel")),
                    0);

            if (option == 0) {
                int migrated = migrate(candidates);
                sendInfoNotification(
                        connection.getProject(),
                        CONNECTION,
                        txt("ntf.credentials.info.ConnectionCredentialRestoreComplete", connection.getName(), migrated));
                if (pending(candidates(settingsManager)).isEmpty()) {
                    setMigrationComplete();
                }
                callback.run();
            } else if (option == 1) {
                prompt(callback, true);
            } else {
                cancel.run();
            }
        });
    }

    private boolean isMigrationComplete() {
        StateAttributes attributes = settingsManager.getStates().getAttributes(SECRET_STORAGE_MIGRATION);
        return attributes != null && attributes.getBooleanAttribute("migrated");
    }

    private void setMigrationComplete() {
        StateAttributes attributes = settingsManager.getStates().ensureAttributes(SECRET_STORAGE_MIGRATION);
        attributes.setBooleanAttribute("migrated", true);
    }

    private void completeCallbacks() {
        callbacks.complete(isMigrationComplete());
    }

    private static List<Candidate> candidates(ProjectSettingsManager settingsManager) {
        List<Candidate> candidates = new ArrayList<>();
        for (ConnectionSettings connection : settingsManager.getConnectionSettings().getConnections()) {
            candidates.addAll(candidates(connection));
        }
        for (AssistantCredential credential :
                settingsManager.getProjectSettings()
                        .getAssistantSettings()
                        .getCredentialSettings()
                        .getCredentials()
                        .getElements()) {
            add(candidates, credential);
        }
        return candidates;
    }

    private static List<Candidate> candidates(ConnectionSettings connection) {
        List<Candidate> candidates = new ArrayList<>();
        ConnectionDatabaseSettings databaseSettings = connection.getDatabaseSettings();
        add(candidates, databaseSettings.getAuthenticationInfo());
        add(candidates, connection.getSshTunnelSettings());
        ReverseSshTunnelConfiguration reverseSshTunnelConfig =
                connection.getDebuggerSettings().getReverseSshTunnelConfig();
        add(candidates, reverseSshTunnelConfig);
        return candidates;
    }

    private static void add(List<Candidate> candidates, SecretsOwner owner) {
        for (Secret secret : owner.getSecrets()) {
            candidates.add(new Candidate(
                    owner,
                    secret.getType(),
                    owner.getSecretOwnerId(),
                    owner.getSecretOwnerName(),
                    secret.getUser()));
        }
    }

    private static int migrate(List<Candidate> candidates) {
        int migrated = 0;
        for (Candidate candidate : pending(candidates)) {
            if (migrateLegacySecret(candidate)) migrated++;
        }
        return migrated;
    }

    private static boolean migrateLegacySecret(Candidate candidate) {
        SecretType secretType = candidate.type();
        String user = candidate.user();

        // check presence of migrated secret
        CredentialAttributes attributes = createAttributes(secretType, candidate.ownerId(), user);
        if (isSecretAvailable(attributes)) {
            candidate.owner().reloadSecrets();
            return false;
        }

        // check presence of legacy secret
        CredentialAttributes legacyAttributes = createAttributes(secretType, candidate.legacyOwnerId(), user);
        if (!isSecretAvailable(legacyAttributes)) {
            return false;
        }

        // migrate legacy credentials
        PasswordSafe passwordSafe = PasswordSafe.getInstance();
        Credentials legacyCredentials = passwordSafe.get(legacyAttributes);
        passwordSafe.set(attributes, legacyCredentials, false);
        candidate.owner().reloadSecrets();
        return true;
    }

    private static List<Candidate> pending(List<Candidate> candidates) {
        return filter(candidates, c -> isMigrationPending(c));
    }

    private static boolean isMigrationPending(Candidate candidate) {
        SecretType type = candidate.type();
        Object ownerId = candidate.ownerId();
        String legacyOwnerId  = candidate.legacyOwnerId();
        String user  = candidate.user();

        if (isSecretAvailable(type, ownerId, user)) return false;
        if (!isSecretAvailable(type, legacyOwnerId, user)) return false;
        return true;
    }

    private static boolean isSecretAvailable(SecretType secretType, Object ownerId, String user) {
        CredentialAttributes attributes = createAttributes(secretType, ownerId, user);
        return isSecretAvailable(attributes);
    }

    private static boolean isSecretAvailable(CredentialAttributes attributes) {
        Credentials credentials = PasswordSafe.getInstance().get(attributes);
        if (credentials == null) return false;

        OneTimeString password = credentials.getPassword();
        if (password == null) return false;
        if (password.isEmpty()) return false;

        return true;
    }

    private static String preview(List<Candidate> candidates) {
        StringBuilder builder = new StringBuilder();
        int limit = Math.min(candidates.size(), 8);
        for (int i = 0; i < limit; i++) {
            Candidate candidate = candidates.get(i);
            builder.append("\n - ")
                    .append(candidate.type().getName())
                    .append(": ")
                    .append(candidate.user())
                    .append("@")
                    .append(candidate.legacyOwnerId());
        }
        if (candidates.size() > limit) {
            builder.append("\n - ")
                    .append(txt("msg.credentials.text.CredentialRestoreMore", candidates.size() - limit));
        }
        return builder.toString();
    }

    private record Candidate(
            SecretsOwner owner,
            SecretType type,
            Object ownerId,
            String legacyOwnerId,
            String user) {}

}
