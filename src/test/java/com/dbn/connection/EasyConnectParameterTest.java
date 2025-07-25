package com.dbn.connection;

import com.dbn.connection.config.EasyConnectParameters;
import com.dbn.connection.config.parameter.CheckForInvalidCharactersValidator;
import com.dbn.connection.config.parameter.RegexConstraintValidator;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class EasyConnectParameterTest {
    private static Map<String,String>  ALL_TCPS_PARAMS;
    @BeforeClass
    public static void testBefore() {
        ALL_TCPS_PARAMS = new HashMap<String, String>();
        EasyConnectParameters.PARAMETER_NAMES.forEach(key -> {
            ALL_TCPS_PARAMS.put(key, "");
        });
        EasyConnectParameters.TCPS_ONLY_PARAMETER_NAMES.forEach(key -> {
            ALL_TCPS_PARAMS.put(key, "");
        });
        ALL_TCPS_PARAMS = Map.copyOf(ALL_TCPS_PARAMS);
    }
    @Test
    public void testRetryDelayValidator() {
        {
            RegexConstraintValidator validator = EasyConnectParameters.RETRY_DELAY_VALIDATOR;

            // valid values return null for the ValidationInfo
            assertNull(validator.validate("fooKey", "1234"));
            assertNull(validator.validate("fooKey", "123msec"));
            assertNull(validator.validate("fooKey", "1234ms"));
            assertNull(validator.validate("fooKey", "123sec"));
            assertNull(validator.validate("fooKey", "123min"));
            assertNull(validator.validate("fooKey", "1234"));
            assertNull(validator.validate("fooKey", "1234 msec"));
            assertNull(validator.validate("fooKey", "1234 ms"));
            assertNull(validator.validate("fooKey", "1234 sec"));
            assertNull(validator.validate("fooKey", "1234 min"));

            // Invalid values return a ValidationInfo object
            assertNotNull(validator.validate("barKey", "a123ms"));
            assertNotNull(validator.validate("barKey", "123secs"));
            assertNotNull(validator.validate("barKey", "123    sec"));
        }

    }

    @Test
    public void testNoDQuotesValidator() {
        CheckForInvalidCharactersValidator validator =
                EasyConnectParameters.NO_DQUOTES_ALLOWED_IN_PROPERTY;
        assertNotNull(validator.validate("barKey", "123  \"  sec"));
    }

    public void testEnsureToParameterString() {
        Map<String, String> params = new HashMap<>();
        LinkedHashMap<String, String> map =
                EasyConnectParameters.ensureParameters(params, DatabaseProtocol.TCPS);
        assertEquals("Too few keys",
                ALL_TCPS_PARAMS.size(),
                map.size());
        assertEquals(ALL_TCPS_PARAMS, map);
        assertFalse(ALL_TCPS_PARAMS == map);

        // not quoted
        map.put("SSL_SERVER_CERT_DN", "CN=adwc.uscom-east-1.oraclecloud.com, O=Oracle Corporation, L=Redwood City, ST=California, C=US");
        map.put("SOURCE_ROUTE", "yes");
        map.put("SSL_SERVER_DN_MATCH", "yes");
        map.put("WALLET_LOCATION", "/Users/foo/dir with spaces");

        testQuotedParameters(map);


        // already quoted
        map.put("WALLET_LOCATION", "\"/Users/foo/dir with spaces\"");
        map.put("SSL_SERVER_CERT_DN", "\"CN=adwc.uscom-east-1.oraclecloud.com, O=Oracle Corporation, L=Redwood City, ST=California, C=US\"");
        testQuotedParameters(map);
    }

    private static void testQuotedParameters(LinkedHashMap<String, String> map) {
        Map<String, String> quotedMap = EasyConnectParameters.ensureParametersIfEasyConnect(
                map, DatabaseProtocol.TCPS, DatabaseUrlType.EZCONNECT, false);
        assertEquals("\"CN=adwc.uscom-east-1.oraclecloud.com, O=Oracle Corporation, L=Redwood City, ST=California, C=US\"",
                quotedMap.get("SSL_SERVER_CERT_DN"));
        assertEquals("\"/Users/foo/dir with spaces\"",
                quotedMap.get("WALLET_LOCATION"));
        quotedMap = EasyConnectParameters.ensureParametersIfEasyConnect(map, DatabaseProtocol.TCPS, DatabaseUrlType.EZCONNECT, true);
        assertEquals("\\\"CN=adwc.uscom-east-1.oraclecloud.com, O=Oracle Corporation, L=Redwood City, ST=California, C=US\\\"",
                quotedMap.get("SSL_SERVER_CERT_DN"));
        assertEquals("\\\"/Users/foo/dir with spaces\\\"",
                quotedMap.get("WALLET_LOCATION"));

        // should remove SSL_* because TCP and not TCPS. Should keep and quote
        // WALLET_LOCATION
        quotedMap = EasyConnectParameters.ensureParametersIfEasyConnect(map, DatabaseProtocol.TCP, DatabaseUrlType.EZCONNECT, false);
        assertNull(quotedMap.get("SSL_SERVER_CERT_DN"));
        assertNull(quotedMap.get("SSL_SERVER_DN_MATCH"));
        assertEquals("yes", quotedMap.get("SOURCE_ROUTE"));
        assertEquals("\"/Users/foo/dir with spaces\"",
                quotedMap.get("WALLET_LOCATION"));
    }
}
