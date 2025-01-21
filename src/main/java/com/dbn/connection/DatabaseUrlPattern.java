/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.connection;

import com.dbn.common.constant.Constants;
import com.dbn.common.database.DatabaseInfo;
import com.dbn.common.database.DatabaseInfo.Default;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.connection.DatabaseUrlPattern.Elements.database;
import static com.dbn.connection.DatabaseUrlPattern.Elements.file;
import static com.dbn.connection.DatabaseUrlPattern.Elements.folder;
import static com.dbn.connection.DatabaseUrlPattern.Elements.host;
import static com.dbn.connection.DatabaseUrlPattern.Elements.port;
import static com.dbn.connection.DatabaseUrlPattern.Elements.profile;
import static com.dbn.connection.DatabaseUrlPattern.Elements.vendor;
import static com.dbn.connection.DatabaseUrlType.CUSTOM;
import static com.dbn.connection.DatabaseUrlType.DATABASE;
import static com.dbn.connection.DatabaseUrlType.FILE;
import static com.dbn.connection.DatabaseUrlType.LDAP;
import static com.dbn.connection.DatabaseUrlType.LDAPS;
import static com.dbn.connection.DatabaseUrlType.SERVICE;
import static com.dbn.connection.DatabaseUrlType.SID;
import static com.dbn.connection.DatabaseUrlType.TNS;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;

@Slf4j
@Getter
@NonNls
public enum DatabaseUrlPattern {

    ORACLE_TNS(
            "jdbc:oracle:thin:@<TNS_PROFILE>?TNS_ADMIN=<TNS_FOLDER>",
            compile("^jdbc:oracle:(thin|oci):@" + profile + "\\?TNS_ADMIN=" + folder + "$", CASE_INSENSITIVE),
            Default.ORACLE, TNS),

    ORACLE_SID(
            "jdbc:oracle:thin:@<HOST>:<PORT>:<DATABASE>",
            compile("^jdbc:oracle:(thin|oci):@" + host + "(:" + port + ")?(:" + database + ")$", CASE_INSENSITIVE),
            Default.ORACLE, SID),

    ORACLE_SERVICE(
            "jdbc:oracle:thin:@//<HOST>:<PORT>/<DATABASE>",
            compile("^jdbc:oracle:(thin|oci):@//" + host + "(:" + port + ")?(/" + database + ")$", CASE_INSENSITIVE),
            Default.ORACLE, SERVICE),


    ORACLE_LDAP(
            "jdbc:oracle:thin:@ldap://<HOST>:<PORT>/<DATABASE>",
            compile("^jdbc:oracle:(thin|oci):@ldap://" + host + "(:" + port + ")?(/" + database + ")$", CASE_INSENSITIVE),
            Default.ORACLE, LDAP),

    ORACLE_LDAPS(
            "jdbc:oracle:thin:@ldaps://<HOST>:<PORT>/<DATABASE>",
            compile("^jdbc:oracle:(thin|oci):@ldaps://" + host + "(:" + port + ")?(/" + database + ")$", CASE_INSENSITIVE),
            Default.ORACLE, LDAPS),

    MYSQL_DB(
            "jdbc:mysql://<HOST>:<PORT>/<DATABASE>",
            compile("^jdbc:mysql://" + host + "(:" + port + ")?(/" + database + ")?$", CASE_INSENSITIVE),
            Default.MYSQL, DATABASE),

    POSTGRES_DB(
            "jdbc:postgresql://<HOST>:<PORT>/<DATABASE>",
            compile("^jdbc:postgresql://" + host + "(:" + port + ")?(/" + database + ")$", CASE_INSENSITIVE),
            Default.POSTGRES, DATABASE),

    REDSHIFT_DB(
            "jdbc:redshift://<HOST>:<PORT>/<DATABASE>",
            compile("^jdbc:redshift://" + host + "(:" + port + ")?(/" + database + ")?" + "?$", CASE_INSENSITIVE),
            Default.POSTGRES, DATABASE),

    SQLITE_FILE(
            "jdbc:sqlite:<FILE>",
            compile("^jdbc:sqlite:" + file + "?$", CASE_INSENSITIVE),
            Default.SQLITE, FILE),

    GENERIC(
            "jdbc:<VENDOR>://<HOST>:<PORT>/<DATABASE>",
            compile("^jdbc:" + vendor + "://" + host + "(:" + port + ")?" + "(/" + database + ")?" + "?$", CASE_INSENSITIVE),
            Default.GENERIC, CUSTOM),
    ;

    interface Elements {
        String vendor = "(?<VENDOR>[\\w\\-.]+)";
        String host = "(?<HOST>[\\w\\-.]+)";
        String port = "(?<PORT>[0-9]{1,100})?";
        String database = "(?<DATABASE>[\\w\\-.$#]+)";
        String profile = "(?<PROFILE>[\\w\\-.]+)";
        String folder = "(?<FOLDER>([a-z]:)?([\\\\/][\\w\\s/_.\\-']+)+)";
        String file = "(?<FILE>([a-z]:)?([\\\\/][\\w\\s/_.\\-']+)+)";
    }


    private final DatabaseUrlType urlType;
    private final String urlTemplate;
    private final Pattern urlPattern;
    private final DatabaseInfo defaultInfo;

    public static DatabaseUrlPattern get(@NotNull DatabaseType databaseType, @NotNull DatabaseUrlType urlType) {
        for (DatabaseUrlPattern urlPattern : values()) {
            if (databaseType.supportsUrlPattern(urlPattern) && urlPattern.getUrlType() == urlType) {
                return urlPattern;
            }
        }
        return databaseType.getDefaultUrlPattern();
    }


    public String buildUrl(DatabaseInfo databaseInfo) {
        return buildUrl(
                databaseInfo.getVendor(),
                databaseInfo.getHost(),
                databaseInfo.getPort(),
                databaseInfo.getDatabase(),
                databaseInfo.getMainFilePath(),
                databaseInfo.ensureTnsFolder(),
                databaseInfo.getTnsProfile());
    }

    public String buildUrl(String vendor, String host, String port, String database, String file, String tnsFolder, String tnsProfile) {
        return urlTemplate.
                replace("<VENDOR>", nvl(vendor, "")).
                replace("<HOST>", nvl(host, "")).
                replace(":<PORT>", isEmpty(port) ? "" : ":" + port).
                replace("<DATABASE>", nvl(database, "")).
                replace("<FILE>", nvl(file, "")).
                replace("<TNS_FOLDER>", nvl(tnsFolder, "")).replaceAll("\\\\", "/").
                replace("<TNS_PROFILE>", nvl(tnsProfile, ""));
    }

    public String getDefaultUrl() {
        return buildUrl(defaultInfo);
    }

    DatabaseUrlPattern(String urlTemplate, Pattern urlPattern, DatabaseInfo defaultInfo, DatabaseUrlType urlType) {
        this.urlTemplate = urlTemplate;
        this.urlPattern = urlPattern;
        this.defaultInfo = defaultInfo;
        this.urlType = urlType;
    }

    @NotNull
    public DatabaseInfo getDefaultInfo() {
        return defaultInfo.clone();
    }

    public String resolveHost(String url) {
        return resolveGroup(url, "HOST", DATABASE, SERVICE, SID, LDAP, LDAPS);
    }

    public String resolvePort(String url) {
        return resolveGroup(url, "PORT", DATABASE, SERVICE, SID, LDAP, LDAPS);
    }

    public String resolveDatabase(String url) {
        return resolveGroup(url, "DATABASE", DATABASE, SERVICE, SID, LDAP, LDAPS);
    }

    public String resolveFile(String url) {
        return resolveGroup(url, "FILE", FILE);
    }

    public String resolveTnsFolder(String url) {
        return resolveGroup(url, "TNS_FOLDER", TNS);
    }

    public String resolveTnsProfile(String url) {
        return resolveGroup(url, "TNS_PROFILE", TNS);
    }

    public boolean isValid(String url) {
        if (isEmpty(url)) return false;

        Matcher matcher = getMatcher(url);
        return matcher.matches();
    }

    @NotNull
    private Matcher getMatcher(String url) {
        return urlPattern.matcher(url);
    }

    private String resolveGroup(String url, String name, DatabaseUrlType ... urlTypes) {
        if (!Constants.isOneOf(urlType, urlTypes)) return "";
        if (!isValid(url)) return "";

        try {
            Matcher matcher = getMatcher(url);
            if (!matcher.matches()) return "";

            return matcher.group(name).trim();
        } catch (Exception e) {
            conditionallyLog(e);
            log.warn("Failed to get group {} from database url", name);
            return "";
        }
    }

    public boolean matches(String url) {
        return isValid(url);
    }
}
