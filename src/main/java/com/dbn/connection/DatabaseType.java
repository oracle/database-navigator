package com.dbn.connection;

import com.dbn.common.constant.Constant;
import com.dbn.common.constant.Constants;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.Presentable;
import com.dbn.common.util.Strings;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Arrays;

import static com.dbn.common.constant.Constant.array;
import static com.dbn.common.util.Strings.toUpperCase;
import static com.dbn.connection.AuthenticationType.NONE;
import static com.dbn.connection.AuthenticationType.OS_CREDENTIALS;
import static com.dbn.connection.AuthenticationType.USER;
import static com.dbn.connection.AuthenticationType.USER_PASSWORD;

@Getter
public enum DatabaseType implements Constant<DatabaseType>, Presentable{
    ORACLE(
            "Oracle",
            Icons.DB_ORACLE,
            Icons.DB_ORACLE_LARGE,
            "oracle.jdbc.driver.OracleDriver",
            AuthenticationType.values(),
            array(DatabaseUrlPattern.ORACLE_SERVICE, DatabaseUrlPattern.ORACLE_SID, DatabaseUrlPattern.ORACLE_TNS, DatabaseUrlPattern.GENERIC)),

    MYSQL(
            "MySQL",
            Icons.DB_MYSQL,
            Icons.DB_MYSQL_LARGE,
            "com.mysql.cj.jdbc.Driver",
            array(NONE, USER, USER_PASSWORD, OS_CREDENTIALS),
            array(DatabaseUrlPattern.MYSQL_DB, DatabaseUrlPattern.GENERIC),
            array("MARIADB", "PERCONA", "OURDELTA", "DRIZZLE", "MAXDB")),

    POSTGRES(
            "PostgreSQL",
            Icons.DB_POSTGRESQL,
            Icons.DB_POSTGRESQL_LARGE,
            "org.postgresql.Driver",
            array(NONE, USER, USER_PASSWORD, OS_CREDENTIALS),
            array(DatabaseUrlPattern.POSTGRES_DB, DatabaseUrlPattern.GENERIC),
            array("REDSHIFT", "BITNINE", "NCLUSTER", "GREENPLUM", "HADOOPDB", "NETEZZA", "PARACCEL", "PGPOOL", "REDHAT", "TORODB", "TERADATA", "YUGABYTE")),

    SQLITE(
            "SQLite",
            Icons.DB_SQLITE,
            Icons.DB_SQLITE_LARGE,
            "org.sqlite.JDBC",
            array(NONE),
            array(DatabaseUrlPattern.SQLITE_FILE, DatabaseUrlPattern.GENERIC)),

    GENERIC(
            "Generic",
            Icons.DB_GENERIC,
            Icons.DB_GENERIC_LARGE,
            "java.sql.Driver",
            array(NONE, USER, USER_PASSWORD, OS_CREDENTIALS),
            array(DatabaseUrlPattern.GENERIC)),

    @Deprecated // used for fallback on existing configs TODO decommission after a few releases
    UNKNOWN(
            "Unknown",
            null,
            null,
            "java.sql.Driver",
            AuthenticationType.values(),
            array(DatabaseUrlPattern.GENERIC));

    private final String name;
    private final Icon icon;
    private final Icon largeIcon;
    private final AuthenticationType[] authTypes;
    private final DatabaseUrlPattern[] urlPatterns;
    private final String driverClassName;
    private String internalLibraryPath;
    private final String[] derivedDbs;

    DatabaseType(
            String name,
            Icon icon,
            Icon largeIcon,
            String driverClassName,
            AuthenticationType[] authTypes,
            DatabaseUrlPattern[] urlPatterns) {
        this(name, icon, largeIcon, driverClassName, authTypes, urlPatterns, array());
    }

    DatabaseType(
            String name,
            Icon icon,
            Icon largeIcon,
            String driverClassName,
            AuthenticationType[] authTypes,
            DatabaseUrlPattern[] urlPatterns,
            String[] derivedDbs) {

        this.name = name;
        this.icon = icon;
        this.largeIcon = largeIcon;
        this.urlPatterns = urlPatterns;
        this.authTypes = authTypes;
        this.driverClassName = driverClassName;
        this.derivedDbs = derivedDbs;
    }

    private boolean isDerivedDb(String identifier) {
        return Arrays.stream(derivedDbs).anyMatch(s -> identifier.contains(s));
    }

    public boolean supportsUrlType(DatabaseUrlType urlType) {
        return getUrlPattern(urlType) != null;
    }

    public boolean supportsUrlPattern(DatabaseUrlPattern pattern) {
        for (DatabaseUrlPattern urlPattern : urlPatterns) {
            if (urlPattern == pattern) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public DatabaseUrlPattern getUrlPattern(DatabaseUrlType urlType) {
        return Arrays.stream(urlPatterns).filter(p -> p.getUrlType() == urlType).findFirst().orElse(null);
    }

    public DatabaseUrlType[] getUrlTypes() {
        DatabaseUrlType[] urlTypes = new DatabaseUrlType[urlPatterns.length];
        for (int i = 0; i < urlPatterns.length; i++) {
            DatabaseUrlPattern urlPattern = urlPatterns[i];
            urlTypes[i] = urlPattern.getUrlType();
        }
        return urlTypes;
    }

    public DatabaseUrlPattern getDefaultUrlPattern() {
        return urlPatterns[0];
    }

    @Nullable
    public DatabaseUrlPattern resolveUrlPattern(String url) {
        if (Strings.isEmpty(url)) return null;

        for (DatabaseUrlPattern urlPattern : urlPatterns) {
            if (urlPattern.matches(url)) {
                return urlPattern;
            }
        }
        return null;
    }

    @NotNull
    public static DatabaseType get(String id) {
        return Constants.get(values(), id, GENERIC);
    }

    public static DatabaseType infer(String url) {
        return Arrays
                .stream(DatabaseType.values())
                .filter(dt -> dt.resolveUrlPattern(url) != null)
                .findFirst()
                .orElse(GENERIC);
    }

    @NotNull
    public static DatabaseType resolve(String ... identifiers) {
        for (String identifier : identifiers) {
            DatabaseType databaseType = strongMatch(identifier);
            if (databaseType != GENERIC) {
                return databaseType;
            }
        }

        return GENERIC;
    }

    public static DatabaseType derive(String ... identifiers) {
        DatabaseType databaseType = resolve(identifiers);
        if (databaseType != GENERIC) return GENERIC;

        for (String identifier : identifiers) {
            databaseType = softMatch(identifier);
            if (databaseType != GENERIC) {
                return databaseType;
            }
        }
        return GENERIC;
    }

    private static DatabaseType strongMatch(String identifier) {
        identifier = identifier == null ? "" : toUpperCase(identifier);
        if (identifier.contains("ORACLE") || identifier.contains("OJDBC")) {
            return DatabaseType.ORACLE;
        } else if (identifier.contains("MYSQL")) {
            return DatabaseType.MYSQL;
        } else if (identifier.contains("POSTGRESQL") || identifier.contains("REDSHIFT")) {
            return DatabaseType.POSTGRES;
        } else if (identifier.contains("SQLITE")) {
            return DatabaseType.SQLITE;
        }
        return GENERIC;
    }


    private static DatabaseType softMatch(String identifier) {
        identifier = identifier == null ? "" : toUpperCase(identifier);
        for (DatabaseType databaseType : values()) {
            if (databaseType.isDerivedDb(identifier)) return databaseType;
        }
        return GENERIC;
    }

    public boolean supportsAuthentication() {
        return authTypes.length > 1 || authTypes[0] != NONE;
    }
}
