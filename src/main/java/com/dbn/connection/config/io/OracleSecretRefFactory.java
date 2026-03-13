package com.dbn.connection.config.io;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;

public final class OracleSecretRefFactory {
    private OracleSecretRefFactory() {}

    public static OracleConnectionJsonConfig.SecretRef base64Password(char[] password) {
        if (password == null || password.length == 0) return null;

        byte[] utf8 = null;
        try {
            ByteBuffer bb = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password));
            utf8 = new byte[bb.remaining()];
            bb.get(utf8);

            String b64 = Base64.getEncoder().encodeToString(utf8);

            return OracleConnectionJsonConfig.SecretRef.builder()
                    .type("base64")
                    .value(b64)
                    .build();
        } finally {
            if (utf8 != null) Arrays.fill(utf8, (byte) 0);
        }
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

        return OracleConnectionJsonConfig.SecretRef.builder()
                .type("base64")
                .value(b64)
                .build();
    }
}