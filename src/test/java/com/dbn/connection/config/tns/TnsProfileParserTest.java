package com.dbn.connection.config.tns;

import com.dbn.test.util.FileUtil;
import org.junit.Test;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class TnsProfileParserTest {
    private static final String TEST_FILE_NAME_TOKEN_PROVIDERS_TNS_NAMES = "tnsnames_token.ora";


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
}
