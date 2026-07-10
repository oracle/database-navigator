package com.dbn.connection.config.export;

import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.database.DatabaseInfo;
import com.dbn.common.util.Strings;
import com.dbn.connection.DatabaseUrlType;
import com.dbn.connection.ServerType;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.ConnectionPropertiesSettings;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.EasyConnectParameters;
import com.dbn.connection.config.tns.TnsNames;
import com.dbn.connection.config.tns.TnsNamesParser;
import com.dbn.connection.config.tns.TnsProfile;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.dbn.common.util.Files.normalizePath;
import static com.dbn.connection.AuthenticationType.USER_PASSWORD;

public class ConfigProviderMapper {
    private ConfigProviderMapper(){}

    public static boolean hasConfiguredWallet(ConnectionSettings settings) {
        if (settings == null) return false;

        DatabaseInfo databaseInfo = settings.getDatabaseSettings().getDatabaseInfo();
        if (hasParameter(databaseInfo.getParameters(), "WALLET_LOCATION")) return true;

        ConnectionPropertiesSettings propertiesSettings = settings.getPropertiesSettings();
        return hasParameter(propertiesSettings.getProperties(), "oracle.net.wallet_location");
    }

    public static ConfigProviderPayload map(ConnectionSettings settings, ConfigProviderExportRequest request) throws Exception{
        ConnectionDatabaseSettings db = settings.getDatabaseSettings();
        DatabaseInfo info = db.getDatabaseInfo();
        AuthenticationInfo auth = db.getAuthenticationInfo();
        ConnectionPropertiesSettings props = settings.getPropertiesSettings();

        String connectDescriptor = resolveConnectDescriptor(info);
        String user = auth == null ? null : trim(auth.getUser());
        Map<String, Object> jdbc = resolveJdbc(props);

        SecretRef passwordRef = auth != null && auth.getType() == USER_PASSWORD ?
                SecretRefFactory.emptyTemplate() :
                null;
        SecretRef walletRef = null;

        if (request != null && request.isIncludeWallet()) {
            Path walletFile = request.getWalletFile();
            walletRef = SecretRefFactory.base64Wallet(walletFile);
        }

        return ConfigProviderPayload.builder()
                .connectDescriptor(connectDescriptor)
                .user(user)
                .password(passwordRef)
                .jdbc(jdbc)
                .walletLocation(walletRef)
                .build();
    }

    private static String resolveConnectDescriptor(DatabaseInfo info) throws Exception {
        if (info == null) return null;

        DatabaseUrlType urlType = info.getUrlType();

        // TNS: export alias
        if (urlType == DatabaseUrlType.TNS) {
            return resolveTnsDescriptor(info);
        }

        // SID/SERVICE/DATABASE/EZCONNECT: build descriptor from fields (preferred)
        String host = trim(info.getHost());
        String port = trim(info.getPort());
        String db   = trim(info.getDatabase());

        if (urlType == DatabaseUrlType.SID ||
                urlType == DatabaseUrlType.SERVICE ||
                urlType == DatabaseUrlType.DATABASE ||
                urlType == DatabaseUrlType.EZCONNECT) {

            validateDescriptorFields(urlType, host, port, db);

            String protocol = info.getProtocol() == null ? "tcp" : info.getProtocol().name().toLowerCase();

            String connectData = buildConnectData(info, urlType, db);
            String descriptionParameters = urlType == DatabaseUrlType.EZCONNECT
                    ? buildDescriptionParameters(info)
                    : "";
            String security = urlType == DatabaseUrlType.EZCONNECT
                    ? buildSecurityParameters(info)
                    : "";

            String descriptor =
                    "(description=" +
                            descriptionParameters +
                            "(address_list=" +
                            "(address=(protocol=" + protocol + ")(host=" + host + ")(port=" + port + "))" +
                            ")" +
                            security +
                            connectData +
                            ")";

            return sanitize(descriptor);
        }

        return null;
    }

    private static String resolveTnsDescriptor(DatabaseInfo info) throws Exception {
        String profileName = trim(info.getTnsProfile());
        if (isBlank(profileName)) {
            throw new IllegalArgumentException("TNS profile is required.");
        }

        String tnsFolder = trim(info.getTnsFolder());
        if (isBlank(tnsFolder)) {
            throw new IllegalArgumentException("TNS folder is required.");
        }

        tnsFolder = normalizePath(tnsFolder);
        File tnsNamesFile = Path.of(tnsFolder, "tnsnames.ora").toFile();
        if (!tnsNamesFile.isFile()) {
            throw new IllegalArgumentException("tnsnames.ora not found in folder: " + tnsFolder);
        }

        TnsNames tnsNames = TnsNamesParser.get(tnsNamesFile);
        for (TnsProfile profile : tnsNames.getProfiles()) {
            if (Strings.equalsIgnoreCase(profile.getProfile(), profileName)) {
                return sanitize(profile.getDescriptor());
            }
        }

        throw new IllegalArgumentException("TNS profile \"" + profileName + "\" was not found in " + tnsNamesFile.getPath());
    }

    private static void validateDescriptorFields(DatabaseUrlType urlType, String host, String port, String database) {
        StringBuilder missing = new StringBuilder();
        appendMissingField(missing, host, "Host");
        appendMissingField(missing, port, "Port");
        appendMissingField(missing, database, urlType == DatabaseUrlType.SID ? "SID" : "Service name");

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Missing required connection fields for " + urlType.name() + ": " + missing);
        }
    }

    private static void appendMissingField(StringBuilder builder, String value, String label) {
        if (!isBlank(value)) return;
        if (!builder.isEmpty()) builder.append(", ");
        builder.append(label);
    }

    private static String normalize(String s) {
        return s == null ? null : s.replaceAll("\\s+", " ").trim();
    }

    private static String buildConnectData(DatabaseInfo info, DatabaseUrlType urlType, String database) {
        StringBuilder builder = new StringBuilder("(connect_data=");
        if (urlType == DatabaseUrlType.SID) {
            builder.append("(sid=").append(database).append(")");
        } else {
            builder.append("(service_name=").append(database).append(")");
        }

        ServerType serverType = info.getServerType();
        if (serverType != null && serverType != ServerType.DEFAULT) {
            builder.append("(server=").append(serverType.name().toLowerCase()).append(")");
        }
        builder.append(")");
        return builder.toString();
    }

    private static String buildDescriptionParameters(DatabaseInfo info) {
        StringBuilder builder = new StringBuilder();
        Map<String, String> parameters = normalizeEasyConnectParameters(info);
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String key = trim(entry.getKey());
            String value = trim(entry.getValue());
            if (isBlank(key) || isBlank(value) || isSecurityParameter(key)) continue;

            builder.append("(")
                    .append(key.toLowerCase())
                    .append("=")
                    .append(value)
                    .append(")");
        }
        return builder.toString();
    }

    private static String buildSecurityParameters(DatabaseInfo info) {
        StringBuilder builder = new StringBuilder();
        Map<String, String> parameters = normalizeEasyConnectParameters(info);
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String key = trim(entry.getKey());
            String value = trim(entry.getValue());
            if (isBlank(key) || isBlank(value) || !isSecurityParameter(key)) continue;

            builder.append("(")
                    .append(key.toLowerCase())
                    .append("=")
                    .append(value)
                    .append(")");
        }

        return builder.isEmpty() ? "" : "(security=" + builder + ")";
    }

    private static Map<String, String> normalizeEasyConnectParameters(DatabaseInfo info) {
        Map<String, String> inputParameters = info.getParameters() == null
                ? Map.of()
                : new LinkedHashMap<>(info.getParameters());

        LinkedHashMap<String, String> parameters = EasyConnectParameters.ensureParameters(
                inputParameters,
                info.getProtocol());

        EasyConnectParameters.ensureQuoted(parameters, false);
        return parameters;
    }

    private static boolean isSecurityParameter(String key) {
        return "WALLET_LOCATION".equalsIgnoreCase(key) ||
                "SSL_SERVER_DN_MATCH".equalsIgnoreCase(key) ||
                "SSL_SERVER_CERT_DN".equalsIgnoreCase(key);
    }

    private static boolean hasParameter(Map<String, String> parameters, String parameterName) {
        if (parameters == null) return false;

        for (Map.Entry<String, String> parameter : parameters.entrySet()) {
            if (parameterName.equalsIgnoreCase(parameter.getKey()) && !isBlank(parameter.getValue())) {
                return true;
            }
        }
        return false;
    }

    private static String trim(String s) { return s == null ? null : s.trim(); }
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }


    private static Map<String, Object> resolveJdbc(ConnectionPropertiesSettings props) {
        if (props == null) return null;

        Map<String, Object> jdbc = new java.util.LinkedHashMap<>();

        // 1) export custom JDBC properties
        Map<String, String> properties = props.getProperties();
        if (properties != null) {
            for (var e : properties.entrySet()) {
                String key = e.getKey();
                String val = e.getValue();

                // enforce spec: do not allow wallet_location inside jdbc
                if ("oracle.net.wallet_location".equalsIgnoreCase(key)) continue;

                jdbc.put(key, coerceNumber(val)); // converts "1000" -> 1000, "20" -> 20, else string
            }
        }

        // 2) export autoCommit exactly as a STRING
        jdbc.put("autoCommit", props.isEnableAutoCommit() ? "true" : "false");

        return jdbc.isEmpty() ? null : jdbc;
    }

    private static Object coerceNumber(String value) {
        if (value == null) return null;
        String v = value.trim();
        if (v.isEmpty()) return "";

        // keep boolean-looking values as strings if present in properties map
        if ("true".equalsIgnoreCase(v) || "false".equalsIgnoreCase(v)) return v;

        try { return Integer.parseInt(v); } catch (Exception ignore) {}
        try { return Long.parseLong(v); } catch (Exception ignore) {}
        try { return Double.parseDouble(v); } catch (Exception ignore) {}

        return value;
    }

    private static String sanitize(String s) {
        s = normalize(s);
        if (s == null) return null;
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            s = s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

}
