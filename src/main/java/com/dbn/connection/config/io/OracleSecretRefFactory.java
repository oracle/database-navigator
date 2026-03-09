package com.dbn.connection.config.io;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public final class OracleSecretRefFactory {
    private OracleSecretRefFactory() {}

    public static OracleConnectionJsonConfig.SecretRef base64Password(char[] password) {
        if (password == null || password.length == 0) return null;

        String plain = new String(password);
        String b64 = Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));

        OracleConnectionJsonConfig.SecretRef ref = new OracleConnectionJsonConfig.SecretRef();
        ref .setType("base64");
        ref.setValue(b64);
        return ref;
    }

    public static OracleConnectionJsonConfig.SecretRef base64Wallet(Path walletFile) throws Exception {
        if (walletFile == null) return null;

        String name = walletFile.getFileName().toString().toLowerCase();
        boolean supported = name.equals("cwallet.sso") || name.equals("ewallet.pem");
        if (!supported) {
            throw new IllegalArgumentException("Unsupported wallet file. Supported: cwallet.sso, ewallet.pem");
        }

        byte[] bytes = Files.readAllBytes(walletFile);
        String b64 = Base64.getEncoder().encodeToString(bytes);

        OracleConnectionJsonConfig.SecretRef ref = new OracleConnectionJsonConfig.SecretRef();
        ref.setType("base64");
        ref.setValue(b64);
        return ref;
    }
}