/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.common.state;

import com.dbn.common.approval.UserApprovalManager;
import com.dbn.common.thread.Background;
import com.dbn.common.util.Chars;
import com.dbn.common.util.Strings;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.credentialStore.OneTimeString;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.progress.ProcessCanceledException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.dbn.credentials.SecretType.STATE_ENCRYPTION_KEY;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
@UtilityClass
public class StateEncryption {
    private static final @NonNls String KEY_USER = "default";
    private static final @NonNls String KEY_OWNER = "state";
    private static final @NonNls String PREFIX = "dbn-enc:v1:";
    private static final @NonNls String CIPHER = "AES/GCM/NoPadding";
    private static final @NonNls String KEY_ALGORITHM = "AES";
    private static final int KEY_SIZE = 32;
    private static final int IV_SIZE = 12;
    private static final int TAG_SIZE = 128;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final AtomicBoolean approvalRequestScheduled = new AtomicBoolean();
    private static volatile boolean encryptionEnabled = true;
    private static SecretKey encryptionKey;

    public static StoredValue encrypt(@NonNls String dataFlavor, @Nullable String value) {
        if (Strings.isEmpty(value)) return new StoredValue(value, false);
        if (!shouldEncrypt()) return new StoredValue(value, false);

        try {
            byte[] iv = new byte[IV_SIZE];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, getEncryptionKey(), new GCMParameterSpec(TAG_SIZE, iv));
            cipher.updateAAD(aad(dataFlavor));

            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return new StoredValue(PREFIX + encode(iv) + ":" + encode(encrypted), true);
        } catch (Exception e) {
            log.warn("Failed to encrypt persistent state value", e);
            return new StoredValue(value, false);
        }
    }

    @Nullable
    public static String decrypt(@NonNls String dataFlavor, @Nullable String value) {
        if (Strings.isEmpty(value)) return value;

        try {
            if (!value.startsWith(PREFIX)) return null;

            String payload = value.substring(PREFIX.length());
            int separator = payload.indexOf(':');
            if (separator == -1) return null;

            byte[] iv = decode(payload.substring(0, separator));
            byte[] encryptedValue = decode(payload.substring(separator + 1));

            return decrypt(iv, encryptedValue, aad(dataFlavor));
        } catch (Exception e) {
            log.warn("Failed to decrypt persistent state value", e);
            return null;
        }
    }

    private static String decrypt(byte[] iv, byte[] encryptedValue, byte[] aad) throws Exception {
        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, getEncryptionKey(), new GCMParameterSpec(TAG_SIZE, iv));
        cipher.updateAAD(aad);

        byte[] decrypted = cipher.doFinal(encryptedValue);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private static synchronized SecretKey getEncryptionKey() {
        if (encryptionKey != null) return encryptionKey;
        encryptionKey = new SecretKeySpec(loadKey(), KEY_ALGORITHM);
        return encryptionKey;
    }

    private static byte[] loadKey() {
        CredentialAttributes attributes = createKeyAttributes();
        byte[] key = loadKey(attributes);
        if (key != null) return key;

        key = new byte[KEY_SIZE];
        RANDOM.nextBytes(key);
        storeKey(attributes, key);
        return key;
    }

    @Nullable
    private static byte[] loadKey(CredentialAttributes attributes) {
        char[] token = loadToken(attributes);
        if (Chars.isEmpty(token)) return null;

        try {
            String encodedKey = new String(token).trim();
            if (Strings.isEmpty(encodedKey)) return null;

            byte[] key = decode(encodedKey);
            if (key.length == KEY_SIZE) return key;

            log.warn("Invalid persistent state encryption key size: {}", key.length);
        } catch (Exception e) {
            log.warn("Invalid persistent state encryption key", e);
        } finally {
            Chars.clear(token);
        }
        return null;
    }

    private static void storeKey(CredentialAttributes attributes, byte[] key) {
        storeToken(attributes, encode(key));
    }

    @Nullable
    private static char[] loadToken(CredentialAttributes attributes) {
        try {
            Credentials credentials = PasswordSafe.getInstance().get(attributes);
            OneTimeString password = credentials == null ? null : credentials.getPassword();
            return password == null ? null : password.toCharArray();
        } catch (Exception e) {
            log.warn("Failed to load persistent state encryption key", e);
            return null;
        }
    }

    private static void storeToken(CredentialAttributes attributes, String token) {
        try {
            Credentials credentials = Strings.isEmpty(token) ? null : new Credentials(KEY_USER, token);
            PasswordSafe.getInstance().set(attributes, credentials, false);
        } catch (Exception e) {
            log.warn("Failed to store persistent state encryption key", e);
        }
    }

    private static CredentialAttributes createKeyAttributes() {
        return new CredentialAttributes(serviceName(), KEY_USER, StateEncryption.class, false);
    }

    @NonNls
    private static String serviceName() {
        return keyServiceName("State encryption key: " + KEY_OWNER);
    }

    @NonNls
    private static String keyServiceName(@NonNls String owner) {
        return "DB Navigator - " + STATE_ENCRYPTION_KEY.getServiceName() + ": " + KEY_USER + "@" + owner;
    }

    private static byte[] aad(@NonNls String dataFlavor) {
        return ("state:" + dataFlavor).getBytes(StandardCharsets.UTF_8);
    }

    @NonNls
    private static String encode(byte[] value) {
        return java.util.Base64.getEncoder().encodeToString(value);
    }

    private static byte[] decode(@NonNls String value) {
        return java.util.Base64.getDecoder().decode(value);
    }

    static boolean shouldEncrypt() {
        return encryptionEnabled && !isMemoryPasswordSafe();
    }

    public static boolean isMemoryPasswordSafe() {
        return /*true || */PasswordSafe.getInstance().isMemoryOnly();
    }

    public static void requestUnencryptedStateApproval() {
        if (!encryptionEnabled) return;
        if (shouldEncrypt()) return;
        if (!approvalRequestScheduled.compareAndSet(false, true)) return;

        Background.run(() -> {
            try {
                ensureUnencryptedStateApproved();
            } finally {
                approvalRequestScheduled.set(false);
            }
        });
    }

    public static boolean ensureUnencryptedStateApproved() {
        if (!encryptionEnabled) return true;
        if (shouldEncrypt()) return true;

        try {
            UserApprovalManager approvalManager = UserApprovalManager.getInstance();
            approvalManager.ensureApproved(StateEncryptionApproval.INSTANCE);
            return true;
        } catch (ProcessCanceledException e) {
            conditionallyLog(e);
            return false;
        }
    }

    public record StoredValue(String value, boolean encrypted) {}
}
