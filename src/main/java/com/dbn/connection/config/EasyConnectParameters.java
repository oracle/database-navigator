package com.dbn.connection.config;

import com.dbn.common.database.DatabaseInfo;
import com.dbn.common.util.Parameters;
import com.dbn.connection.DatabaseProtocol;
import com.dbn.connection.DatabaseUrlType;
import com.dbn.connection.config.parameter.CheckForInvalidCharactersValidator;
import com.dbn.connection.config.parameter.RegexConstraintValidator;
import com.dbn.language.common.quotes.QuotePair;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NonNls;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.common.util.Strings.trim;
import static com.dbn.connection.DatabaseUrlType.EZCONNECT;
import static com.dbn.nls.NlsResources.txt;

/**
 * Common constants and utility functionality to support Easy Connect
 * Parameters.  See https://download.oracle.com/ocomdocs/global/Oracle-Net-Easy-Connect-Plus.pdf
 */
public class EasyConnectParameters {
    /**
     * List of all general Easy Connect parameters that we support.
     */
    @NonNls
    public static final List<String> PARAMETER_NAMES = List.of(
            "ENABLE",
            "FAILOVER",
            "LOAD_BALANCE",
            "RECV_BUF_SIZE",
            "SEND_BUF_SIZE",
            "SDU",
            "SOURCE_ROUTE",
            "RETRY_COUNT",
            "RETRY_DELAY",
            "HTTPS_PROXY",
            "HTTPS_PROXY_PORT",
            "WALLET_LOCATION");

    /**
     * Easy Connect parameters only available when protocol is TCPS.
     */
    @NonNls
    public static final List<String> TCPS_ONLY_PARAMETER_NAMES = List.of(
            "SSL_SERVER_DN_MATCH",
            "SSL_SERVER_CERT_DN");

    /**
     * List of property values that represent valid boolean type values.
     */
    @NonNls
    public static final List<String> BOOLEAN_LIKE_STRING_VALUES = List.of(
            "on", "off", "ON", "OFF", "true", "false", "TRUE", "FALSE", "yes", "no", "YES", "NO");

    /**
     * List of parameters that must have their values quoted in the connect URL.
     */
    @NonNls
    public static final List<String> PARAMETERS_THAT_NEED_QUOTING = List.of(
            "WALLET_LOCATION", "SSL_SERVER_CERT_DN");

    /**
     * Error message for RETRY_DELAY.
     */
    public static final String RETRY_DELAY_SHOULD_MATCH = txt("cfg.connection.token.RetryDelayFormat");

    /**
     * Common validator pattern instance for RETRY_DELAY.
     */
    public final static RegexConstraintValidator.ValidationPattern RETRY_DELAY_PATTERN =
            new RegexConstraintValidator.ValidationPattern("\\d+( )?(ms|msec|sec|min)?", RETRY_DELAY_SHOULD_MATCH);

    /**
     * Common validator instance for RETRY_DELAY.
     */
    public final static RegexConstraintValidator RETRY_DELAY_VALIDATOR = new RegexConstraintValidator(RETRY_DELAY_PATTERN);

    /**
     * Common validator for properties where we don't want double quotes in the value.
     */
    public final static CheckForInvalidCharactersValidator NO_DQUOTES_ALLOWED_IN_PROPERTY =
            new CheckForInvalidCharactersValidator(Set.of(Character.valueOf('"')),
                    Optional.of(QuotePair.DEFAULT_IDENTIFIER_QUOTE_PAIR::unquote));

    /**
     * Returns the complete Easy Connect parameter set for the protocol, initialized with blank values and
     * overlaid with the supplied parameter values.
     *
     * @param parameters existing parameter values
     * @param protocol database protocol controlling whether TCPS-only parameters are included
     * @return a copy of parameters adjusted for whether the connection uses TCPS
     */
    public static LinkedHashMap<String, String> ensureParameters(Map<String, String> parameters, DatabaseProtocol protocol) {
        LinkedHashMap<String, String> copyOfParameters = new LinkedHashMap<>();
        PARAMETER_NAMES.forEach(key -> copyOfParameters.put(key, ""));
        if (protocol == DatabaseProtocol.TCPS) {
            TCPS_ONLY_PARAMETER_NAMES.forEach(key -> copyOfParameters.put(key, ""));
        }
        copyOfParameters.putAll(parameters);
        // if not TCPS, remove any key/value pairs that aren't appropriate in the copy.
        if (protocol!= DatabaseProtocol.TCPS) {
            TCPS_ONLY_PARAMETER_NAMES.forEach((key) -> parameters.remove(key));
        }
        return copyOfParameters;
    }

    /**
     * Returns a sanitized copy of Easy Connect parameters, excluding unknown parameters and
     * key/value pairs that can introduce additional URL query tokens.
     *
     * @param parameters parameter values to sanitize
     * @param protocol database protocol controlling whether TCPS-only parameters are allowed
     * @return sanitized Easy Connect parameter values
     */
    public static Map<String, String> sanitizeParameters(Map<String, String> parameters, DatabaseProtocol protocol) {
        return Parameters.sanitizeParameters(parameters, supportedParameterNames(protocol), PARAMETERS_THAT_NEED_QUOTING);
    }

    /**
     * Returns Easy Connect parameter names supported for the supplied protocol.
     *
     * @param protocol database protocol controlling whether TCPS-only parameters are included
     * @return supported Easy Connect parameter names
     */
    private static Set<String> supportedParameterNames(DatabaseProtocol protocol) {
        Set<String> supportedParameterNames = new HashSet<>(PARAMETER_NAMES);
        if (protocol == DatabaseProtocol.TCPS) {
            supportedParameterNames.addAll(TCPS_ONLY_PARAMETER_NAMES);
        }
        return supportedParameterNames;
    }

    /**
     * Returns a copy of parameters adjusted for Easy Connect quoting and protocol-specific availability.
     *
     * @param parameters existing parameter values
     * @param databaseInfo database information providing protocol and URL type
     * @param escapeQuotes whether generated quote characters should be escaped
     * @return a copy of parameters adjusted when the URL type is Easy Connect
     */
    public static Map<String, String> ensureParametersIfEasyConnect(Map<String,String> parameters, DatabaseInfo databaseInfo, boolean escapeQuotes) {
        return ensureParametersIfEasyConnect(parameters, databaseInfo.getProtocol(), databaseInfo.getUrlType(), escapeQuotes);
    }

    /**
     * Returns a copy of parameters adjusted for Easy Connect quoting and protocol-specific availability.
     *
     * @param parameters existing parameter values
     * @param protocol database protocol controlling whether TCPS-only parameters are retained
     * @param urlType database URL type controlling whether Easy Connect adjustments apply
     * @param escapeQuotes whether generated quote characters should be escaped
     * @return a copy of parameters adjusted when the URL type is Easy Connect
     */
    public static Map<String, String> ensureParametersIfEasyConnect(Map<String,String> parameters, DatabaseProtocol protocol, DatabaseUrlType urlType, boolean escapeQuotes) {
        Map<String, String> copyOfParameters = new HashMap<>(parameters);
        if (urlType == EZCONNECT) {
            return ensureQuoted(sanitizeParameters(copyOfParameters, protocol), escapeQuotes);
        }
        return copyOfParameters;
    }

    /**
     * Quotes Easy Connect parameter values that require URL-level quoting.
     *
     * @param parameters parameter values to modify
     * @param escapeQuotes whether generated quote characters should be escaped
     * @return the supplied parameter map with quote-required values double-quoted when necessary
     */
    public static Map<String, String> ensureQuoted(Map<String, String> parameters, boolean escapeQuotes) {
        PARAMETERS_THAT_NEED_QUOTING.forEach(key -> {
            if (!parameters.containsKey(key)) return;

            String originalValue = trim(parameters.get(key));
            if (isEmpty(originalValue)) return;

            originalValue = QuotePair.DEFAULT_IDENTIFIER_QUOTE_PAIR.quote(originalValue);
            if (escapeQuotes) {
                originalValue = StringUtil.escapeStringCharacters(originalValue);
            }
            parameters.put(key, originalValue);
        });
        return parameters;
    }
}
