/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.connection.config.export;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class SecretRefFactoryTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void base64WalletEncodesSupportedWalletFile() throws Exception {
        Path wallet = temporaryFolder.newFile("cwallet.sso").toPath();
        Files.write(wallet, new byte[] {0, 1, 2, 3});

        SecretRef ref = SecretRefFactory.base64Wallet(wallet);

        assertEquals(SecretProviderType.BASE64, ref.getType());
        assertEquals("AAECAw==", ref.getValue());
    }

    @Test
    public void base64WalletRejectsPemWalletFile() throws Exception {
        Path wallet = temporaryFolder.newFile("ewallet.pem").toPath();

        assertThrows(IllegalArgumentException.class, () -> SecretRefFactory.base64Wallet(wallet));
    }

    @Test
    public void base64WalletRejectsUnsupportedWalletFileName() throws Exception {
        Path wallet = temporaryFolder.newFile("wallet.zip").toPath();

        assertThrows(IllegalArgumentException.class, () -> SecretRefFactory.base64Wallet(wallet));
    }
}
