package com.dbn.connection.config.io;

import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.database.DatabaseInfo;
import com.dbn.connection.DatabaseUrlType;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.config.ConnectionPropertiesSettings;
import com.dbn.connection.config.ConnectionSettings;

import java.util.Map;

public final class OracleConnectionJsonMapper {
    private OracleConnectionJsonMapper() {}

    public static OracleConnectionJsonConfig from(ConnectionSettings settings) {

        ConnectionDatabaseSettings db = settings.getDatabaseSettings();
        DatabaseInfo info = db.getDatabaseInfo();
        AuthenticationInfo auth = db.getAuthenticationInfo();
        ConnectionPropertiesSettings props = settings.getPropertiesSettings();

        return OracleConnectionJsonConfig.builder()
                .connectDescriptor(resolveConnectDescriptor(info))
                .user(auth == null || auth.getUser() == null || auth.getUser().isBlank() ? null : auth.getUser())
                .jdbc(resolveJdbc(props))
                .password(null)
                .walletLocation(null)
                .build();
    }

    public static OracleConnectionJsonConfig.OracleConnectionJsonConfigBuilder builderFrom(ConnectionSettings settings) {
        ConnectionDatabaseSettings db = settings.getDatabaseSettings();
        DatabaseInfo info = db.getDatabaseInfo();
        AuthenticationInfo auth = db.getAuthenticationInfo();
        ConnectionPropertiesSettings props = settings.getPropertiesSettings();

        return OracleConnectionJsonConfig.builder()
                .connectDescriptor(resolveConnectDescriptor(info))  // required
                .user(auth == null || auth.getUser() == null || auth.getUser().isBlank() ? null : auth.getUser())
                .jdbc(resolveJdbc(props));
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

        if (!isBlank(host) && !isBlank(port) && !isBlank(db) &&
                (urlType == DatabaseUrlType.SID || urlType == DatabaseUrlType.SERVICE || urlType == DatabaseUrlType.DATABASE)) {

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

        // CUSTOM (or fallback): return base if present
        if (!isBlank(url)) {
            String remainder = stripJdbcPrefix(url); // everything after '@'
            remainder = trim(remainder);
            if (isBlank(remainder)) return null;

            // case: jdbc:oracle:thin:@?TNS_ADMIN=...  -> no connect descriptor
            if (remainder.startsWith("?")) return null;

            // case: jdbc:oracle:thin:@(description=...) -> keep as-is
            if (startsLikeDescriptor(remainder)) return sanitize(remainder);

            // default: drop query params (e.g. TNS_ADMIN) and return base
            String base = beforeQuery(remainder);
            return isBlank(base) ? null : sanitize(base);
        }

        return null;
    }

    private static String stripJdbcPrefix(String url) {
        String prefix = "jdbc:oracle:thin:@";
        String s = url.trim();
        return s.startsWith(prefix) ? s.substring(prefix.length()).trim() : s;
    }

    private static String beforeQuery(String s) {
        if (s == null) return null;
        int idx = s.indexOf('?');
        return idx == -1 ? s.trim() : s.substring(0, idx).trim();
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

    private static boolean startsLikeDescriptor(String s) {
        String v = trim(s);
        return v != null && v.regionMatches(true, 0, "(description=", 0, "(description=".length());
    }

}