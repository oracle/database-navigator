package com.dbn.connection.config.export;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;

public class SecretRefFactory {

    private SecretRefFactory(){}

    public static SecretRef base64Password(char[] password) {
        if (password == null || password.length == 0) return null;

        ByteBuffer buffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(password));
        try {
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            try {
                return SecretRef.builder()
                        .type(SecretProviderType.BASE64)
                        .value(Base64.getEncoder().encodeToString(bytes))
                        .build();
            } finally {
                Arrays.fill(bytes, (byte) 0);
            }
        } finally {
            if (buffer.hasArray()) Arrays.fill(buffer.array(), (byte) 0);
        }
    }

    public static SecretRef base64Wallet(Path walletFile) throws Exception {
        if (walletFile == null) return null;

        String name = walletFile.getFileName().toString().toLowerCase();
        boolean supported = name.equals("cwallet.sso");
        if (!supported) {
            throw new IllegalArgumentException("Unsupported wallet file. Supported: cwallet.sso");
        }

        byte[] bytes = Files.readAllBytes(walletFile);
        try {
            return SecretRef.builder()
                    .type(SecretProviderType.BASE64)
                    .value(Base64.getEncoder().encodeToString(bytes))
                    .build();
        } finally {
            Arrays.fill(bytes, (byte) 0);
        }
    }
}
