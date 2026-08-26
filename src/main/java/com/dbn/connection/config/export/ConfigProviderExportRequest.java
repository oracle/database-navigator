package com.dbn.connection.config.export;

import com.dbn.common.export.ExportDestination;
import lombok.Builder;
import lombok.Value;

import java.nio.file.Path;

import static com.dbn.common.util.Passwords.clearPassword;

@Value
@Builder
public class ConfigProviderExportRequest {
    private ExportDestination destination;
    private Path outputFile;
    private Path walletFile;
    private String formatId;
    private String wrapperKey;
    private boolean includeWallet;
    private boolean includeDatabasePassword;
    private char[] databasePassword;

    public void clearDatabasePassword() {
        clearPassword(databasePassword);
    }
}
