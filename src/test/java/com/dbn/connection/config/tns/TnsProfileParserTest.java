package com.dbn.connection.config.tns;

import com.dbn.test.util.FileUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TnsProfileParserTest {
    private static final String TEST_FILE_NAME_TOKEN_PROVIDERS_TNS_NAMES = "tnsnames_token.ora";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();


    @Test
    public void testTokenProvidersTnsNames() {
        Set<String> expectedProfiles = Set.of("azure_db", "azure_interactive", "azure_device_code",
                "azure_service_principal", "azure_service_principal_secret","azure_service_principal_wallet",
                "oci_db", "oci_interactive", "oci_api_key");
        File tnsNamesFile = FileUtil.getFileFromClasspath(getClass(), TEST_FILE_NAME_TOKEN_PROVIDERS_TNS_NAMES);
        TnsNames tnsNames = TnsNamesParser.parse(tnsNamesFile);
        Set<String> actualProfiles = new HashSet<>(tnsNames.getProfileNames());
        assertEquals(expectedProfiles, actualProfiles);
    }

    @Test
    public void acceptsMaximumTnsNamesFileSize() throws IOException {
        File tnsNamesFile = temporaryFolder.newFile("maximum-tnsnames.ora");
        try (RandomAccessFile file = new RandomAccessFile(tnsNamesFile, "rw")) {
            file.setLength(TnsNamesParser.MAX_FILE_SIZE_BYTES);
        }

        assertEquals(0, TnsNamesParser.parse(tnsNamesFile).size());
    }

    @Test
    public void rejectsOversizedTnsNamesFile() throws IOException {
        File tnsNamesFile = temporaryFolder.newFile("oversized-tnsnames.ora");
        try (RandomAccessFile file = new RandomAccessFile(tnsNamesFile, "rw")) {
            file.setLength(TnsNamesParser.MAX_FILE_SIZE_BYTES + 1);
        }

        IOException exception = assertThrows(IOException.class, () -> TnsNamesParser.parse(tnsNamesFile));
        assertTrue(exception.getMessage().contains("10"));
    }
}
