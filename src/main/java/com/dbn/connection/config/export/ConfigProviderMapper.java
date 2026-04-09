package com.dbn.connection.config.export;

import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.database.DatabaseInfo;
import com.dbn.connection.DatabaseUrlType;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.ConnectionPropertiesSettings;
import com.dbn.connection.config.ConnectionSettings;

import java.nio.file.Path;
import java.util.Map;

public class ConfigProviderMapper {
    private ConfigProviderMapper(){}

    public static ConfigProviderPayload map(ConnectionSettings settings, ConfigProviderExportRequest request) throws Exception{
        ConnectionDatabaseSettings db = settings.getDatabaseSettings();
        DatabaseInfo info = db.getDatabaseInfo();
        AuthenticationInfo auth = db.getAuthenticationInfo();
        ConnectionPropertiesSettings props = settings.getPropertiesSettings();

        String connectDescriptor = resolveConnectDescriptor(info);
        String user = auth == null ? null : trim(auth.getUser());
        Map<String, Object> jdbc = resolveJdbc(props);

        SecretRef walletRef = null;

        if (request != null && request.isIncludeWallet()) {
            Path walletFile = request.getWalletFile();
            walletRef = SecretRefFactory.base64Wallet(walletFile);
        }

        return ConfigProviderPayload.builder()
                .connectDescriptor(connectDescriptor)
                .user(user)
                .jdbc(jdbc)
                .walletLocation(walletRef)
                .build();
    }

    private static String resolveConnectDescriptor(DatabaseInfo info) {
        if (info == null) return null;

        DatabaseUrlType urlType = info.getUrlType();
        String url = trim(info.getUrl());

        // TNS: export alias
        if (urlType == DatabaseUrlType.TNS) {
            String profile = trim(info.getTnsProfile());
            return isBlank(profile) ? null : sanitize(profile);
        }

        // SID/SERVICE/DATABASE: build descriptor from fields (preferred)
        String host = trim(info.getHost());
        String port = trim(info.getPort());
        String db   = trim(info.getDatabase());

        if (urlType == DatabaseUrlType.SID ||
                urlType == DatabaseUrlType.SERVICE ||
                urlType == DatabaseUrlType.DATABASE) {

            if (isBlank(host) || isBlank(port) || isBlank(db)) {
                return null; // block export: missing required fields
            }

            String protocol = info.getProtocol() == null ? "tcp" : info.getProtocol().name().toLowerCase();

            String connectData = (urlType == DatabaseUrlType.SID)
                    ? "(connect_data=(sid=" + db + "))"
                    : "(connect_data=(service_name=" + db + "))";

            String descriptor =
                    "(description=" +
                            "(address_list=" +
                            "(address=(protocol=" + protocol + ")(host=" + host + ")(port=" + port + "))" +
                            ")" +
                            connectData +
                            ")";

            return sanitize(descriptor);
        }

        return null;
    }

    private static String stripJdbcPrefix(String url) {
        String prefix = "jdbc:oracle:thin:@";
        String s = url.trim();
        return s.startsWith(prefix) ? s.substring(prefix.length()).trim() : s;
    }

    private static String normalize(String s) {
        return s == null ? null : s.replaceAll("\\s+", " ").trim();
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
