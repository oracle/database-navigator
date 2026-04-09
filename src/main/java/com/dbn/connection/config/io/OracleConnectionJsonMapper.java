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
        OracleConnectionJsonConfig cfg = new OracleConnectionJsonConfig();

        ConnectionDatabaseSettings db = settings.getDatabaseSettings();
        DatabaseInfo info = db.getDatabaseInfo();
        AuthenticationInfo auth = db.getAuthenticationInfo();
        ConnectionPropertiesSettings props = settings.getPropertiesSettings();

        cfg.setConnectDescriptor(resolveConnectDescriptor(info));  // REQUIRED
        cfg.setUser(auth == null ||auth.getUser().equals("") ? null : auth.getUser());         // optional
        cfg.setJdbc(resolveJdbc(props));                           // optional

        cfg.setPassword(null);
        cfg.setWalletLocation(null);

        return cfg;
    }

    private static String resolveConnectDescriptor(DatabaseInfo info) {
        if (info == null) return null;

        DatabaseUrlType urlType = info.getUrlType();
        String url = trim(info.getUrl());

        // TNS: export alias
        if (urlType == DatabaseUrlType.TNS) {
            String profile = trim(info.getTnsProfile());
            return isBlank(profile) ? null : profile;
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

            return normalize(descriptor);
        }

        // CUSTOM (or fallback): return base if present
        if (!isBlank(url)) {
            String base = beforeQuery(stripJdbcPrefix(url));
            return isBlank(base) ? null : normalize(base);
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

}