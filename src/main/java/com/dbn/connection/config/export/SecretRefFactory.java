package com.dbn.connection.config.export;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class SecretRefFactory {

    private SecretRefFactory(){}

    public static SecretRef base64Wallet(Path walletFile) throws Exception {
        if (walletFile == null) return null;

        String name = walletFile.getFileName().toString().toLowerCase();
        boolean supported = name.equals("cwallet.sso") || name.equals("ewallet.pem");
        if (!supported) {
            throw new IllegalArgumentException("Unsupported wallet file. Supported: cwallet.sso, ewallet.pem");
        }

        byte[] bytes = Files.readAllBytes(walletFile);
        String b64 = Base64.getEncoder().encodeToString(bytes);

        return SecretRef.builder()
                .type(SecretProviderType.BASE64)
                .value(b64)
                .build();
    }
}
