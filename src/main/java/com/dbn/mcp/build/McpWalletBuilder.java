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

package com.dbn.mcp.build;

import com.dbn.common.exception.Exceptions;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionUtil;
import com.dbn.mcp.model.OracleSecretStore;
import com.dbn.mcp.model.OracleWallet;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.Driver;

import static com.dbn.common.util.Passwords.clearPassword;
import static com.dbn.nls.NlsResources.txt;

@RequiredArgsConstructor
final class McpWalletBuilder {
    private static final @NonNls String DEFAULT_SEPS_USERNAME = "oracle.security.client.default_username";
    private static final @NonNls String DEFAULT_SEPS_PASSWORD = "oracle.security.client.default_password";

    private final ConnectionHandler connection;

    void build(Path dir) throws IOException {
        Path walletDir = dir.resolve("wallet");
        Files.createDirectories(walletDir);

        char[] user = safe(connection.getUserName()).toCharArray();
        char[] password = getPassword(connection);

        // Random password - used only to create ewallet.p12, never stored or shown.
        // cwallet.sso (used at runtime) needs no password.
        char[] walletPassword = generateWalletPassword();

        try {
            ClassLoader classLoader = getWalletClassLoader();
            OracleWallet wallet = OracleWallet.newInstance(classLoader);
            wallet.create(walletPassword);
            wallet.setLocation(walletDir.toAbsolutePath().toString());

            OracleSecretStore store = wallet.getSecretStore();
            // Use documented default SEPS keys to avoid connect-string lookup mismatches.
            store.setSecret(DEFAULT_SEPS_USERNAME, user);
            store.setSecret(DEFAULT_SEPS_PASSWORD, password);
            wallet.setSecretStore(store);

            wallet.save();
            wallet.saveSSO();
        } catch (Exception e) {
            Throwable root = Exceptions.rootCauseOf(Exceptions.unwrap(e));
            String message = root != null && root.getMessage() != null && !root.getMessage().isBlank()
                    ? root.getMessage()
                    : e.getClass().getSimpleName();
            throw new IOException(txt("msg.mcp.exception.OracleSepsWalletCreationFailed", message), e);
        } finally {
            clearPassword(user); // TODO do we need this?
            clearPassword(password);
            clearPassword(walletPassword);
        }
    }

    private ClassLoader getWalletClassLoader() throws Exception {
        ClassLoader driverClassLoader = getDriverClassLoader();
        if (driverClassLoader != null && containsClass(driverClassLoader, "oracle.security.pki.OracleWallet")) {
            return driverClassLoader;
        }

        throw new ClassNotFoundException(txt("msg.mcp.exception.OraclePkiLibraryMissing"));
    }

    private static boolean containsClass(ClassLoader classLoader, String className) {
        try {
            Class.forName(className, false, classLoader);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private ClassLoader getDriverClassLoader() throws Exception {
        Driver driver = ConnectionUtil.resolveDriver(connection.getSettings().getDatabaseSettings());
        return driver == null ? null : driver.getClass().getClassLoader();
    }

    private static char[] generateWalletPassword() {
        SecureRandom rng = new SecureRandom();
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String chars = letters + digits;
        char[] password = new char[32];
        password[0] = letters.charAt(rng.nextInt(letters.length()));
        password[1] = digits.charAt(rng.nextInt(digits.length()));
        for (int i = 2; i < password.length; i++) {
            password[i] = chars.charAt(rng.nextInt(chars.length()));
        }
        for (int i = password.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            char tmp = password[i];
            password[i] = password[j];
            password[j] = tmp;
        }
        return password;
    }

    private static char[] getPassword(ConnectionHandler connection) {
        if (connection == null || connection.getAuthenticationInfo() == null) return new char[0];
        char[] password = connection.getAuthenticationInfo().getPassword();
        return password != null ? password.clone() : new char[0];
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }
}
