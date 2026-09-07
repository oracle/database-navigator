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

import com.dbn.common.component.ConnectionComponent;
import com.dbn.common.exception.Exceptions;
import com.dbn.common.util.FilePermissions;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionUtil;
import com.dbn.mcp.model.OracleSecretStore;
import com.dbn.mcp.model.OracleWallet;
import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.Driver;
import java.util.stream.Stream;

import static com.dbn.common.util.Passwords.clearPassword;
import static com.dbn.nls.NlsResources.txt;

final class McpWalletBuilder extends ConnectionComponent {
    private static final @NonNls String DEFAULT_SEPS_USERNAME = "oracle.security.client.default_username";
    private static final @NonNls String DEFAULT_SEPS_PASSWORD = "oracle.security.client.default_password";
    private static final @NonNls String AUTO_LOGIN_WALLET = "cwallet.sso";
    private static final @NonNls String PASSWORD_WALLET = "ewallet.p12";

    McpWalletBuilder(ConnectionHandler connection) {
        super(connection);
    }

    void build(Path dir) throws IOException {
        Path walletDir = dir.resolve("wallet").toAbsolutePath().normalize();
        boolean walletDirectoryExisted = prepareWalletDirectory(walletDir);

        char[] user = new char[0];
        char[] password = new char[0];
        char[] walletPassword = new char[0];

        try {
            ConnectionHandler connection = getConnection();
            user = safe(connection.getUserName()).toCharArray();
            password = getPassword(connection);

            // Random password - used only to create ewallet.p12, never stored or shown.
            // cwallet.sso (used at runtime) needs no password.
            walletPassword = generateWalletPassword();

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
            secureWalletContents(walletDir);
            deleteIntermediateWalletFiles(walletDir);
        } catch (Exception e) {
            if (!walletDirectoryExisted) {
                try {
                    deleteDirectory(walletDir);
                } catch (IOException cleanupFailure) {
                    e.addSuppressed(cleanupFailure);
                }
            }
            Throwable root = Exceptions.rootCauseOf(Exceptions.unwrap(e));
            String message = root != null && root.getMessage() != null && !root.getMessage().isBlank()
                    ? root.getMessage()
                    : e.getClass().getSimpleName();
            throw new IOException(txt("msg.mcp.exception.OracleSepsWalletCreationFailed", message), e);
        } finally {
            clearPassword(user);
            clearPassword(password);
            clearPassword(walletPassword);
        }
    }

    private static boolean prepareWalletDirectory(Path walletDir) throws IOException {
        Files.createDirectories(walletDir.getParent());
        if (Files.isSymbolicLink(walletDir)) {
            throw new IOException("Refusing to create an Oracle wallet through a symbolic link: " + walletDir);
        }

        boolean existed = Files.exists(walletDir, LinkOption.NOFOLLOW_LINKS);
        try {
            try {
                Files.createDirectory(walletDir, FilePermissions.ownDirectoryPermissions());
            } catch (FileAlreadyExistsException ignored) {
                // The output directory may already contain a wallet from an earlier build.
            } catch (UnsupportedOperationException ignored) {
                // Non-POSIX providers are restricted through their ACL or legacy file API below.
                try {
                    Files.createDirectory(walletDir);
                } catch (FileAlreadyExistsException ignoredAgain) {
                    // Another process may have created the directory between the checks.
                }
            }

            if (!Files.isDirectory(walletDir, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Oracle wallet path is not a directory: " + walletDir);
            }
            secureWalletContents(walletDir);
            return existed;
        } catch (IOException | RuntimeException e) {
            if (!existed) {
                try {
                    deleteDirectory(walletDir);
                } catch (IOException cleanupFailure) {
                    e.addSuppressed(cleanupFailure);
                }
            }
            throw e;
        }
    }

    private static void secureWalletContents(Path walletDir) throws IOException {
        try (Stream<Path> stream = Files.walk(walletDir)) {
            for (Path path : stream.toList()) {
                if (Files.isSymbolicLink(path)) {
                    throw new IOException("Refusing to use a symbolic link in the Oracle wallet: " + path);
                }

                if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) ||
                        Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    FilePermissions.restrictToOwner(path);
                }
            }
        }
    }

    private static void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) return;
        if (Files.isSymbolicLink(directory)) {
            throw new IOException("Refusing to delete a symbolic link as an Oracle wallet: " + directory);
        }

        try (Stream<Path> stream = Files.walk(directory)) {
            for (Path path : stream.sorted(java.util.Comparator.reverseOrder()).toList()) {
                if (Files.isSymbolicLink(path)) {
                    throw new IOException("Refusing to delete a symbolic link in the Oracle wallet: " + path);
                }
                Files.deleteIfExists(path);
            }
        }
    }

    private static void deleteIntermediateWalletFiles(Path walletDir) throws IOException {
        Files.deleteIfExists(walletDir.resolve(PASSWORD_WALLET));
        Files.deleteIfExists(walletDir.resolve(PASSWORD_WALLET + ".lck"));
        Files.deleteIfExists(walletDir.resolve(AUTO_LOGIN_WALLET + ".lck"));
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
        Driver driver = ConnectionUtil.resolveDriver(getConnection().getSettings().getDatabaseSettings());
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
