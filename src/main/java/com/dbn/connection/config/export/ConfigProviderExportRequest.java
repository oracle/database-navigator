package com.dbn.connection.config.export;

import lombok.Builder;
import lombok.Value;

import java.nio.file.Path;

@Value
@Builder
public class ConfigProviderExportRequest {
    public enum Destination {
        FILE,
        CLIPBOARD
    }

    Path outputFile;
    Destination destination;
    String formatId;
    String wrapperKey;
    boolean includeWallet;
    Path walletFile;
}
