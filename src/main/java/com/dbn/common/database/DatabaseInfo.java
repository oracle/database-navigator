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

package com.dbn.common.database;


import com.dbn.common.util.Cloneable;
import com.dbn.common.util.Strings;
import com.dbn.connection.DatabaseUrlPattern;
import com.dbn.connection.DatabaseUrlType;
import com.dbn.connection.config.file.DatabaseFile;
import com.dbn.connection.config.file.DatabaseFileBundle;
import com.dbn.connection.config.tns.TnsAdmin;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static com.dbn.connection.DatabaseUrlType.CUSTOM;
import static com.dbn.connection.DatabaseUrlType.DATABASE;
import static com.dbn.connection.DatabaseUrlType.FILE;
import static com.dbn.connection.DatabaseUrlType.SID;

@NonNls
@Getter
@Setter
@EqualsAndHashCode
public class DatabaseInfo implements Cloneable<DatabaseInfo> {

    public interface Default {
        DatabaseInfo ORACLE   = new DatabaseInfo("oracle", "localhost", "1521", "XE", SID);
        DatabaseInfo MYSQL    = new DatabaseInfo("mysql", "localhost", "3306", "mysql", DATABASE);
        DatabaseInfo POSTGRES = new DatabaseInfo("postgresql", "localhost", "5432", "postgres", DATABASE);
        DatabaseInfo SQLITE   = new DatabaseInfo("sqlite", "sqlite.db", FILE);
        DatabaseInfo GENERIC  = new DatabaseInfo("dbtype", "localhost", "1234", "database", DATABASE);
    }

    private String vendor;
    private String host;
    private String port;
    private String database;
    private String url;
    private DatabaseFileBundle fileBundle;
    private DatabaseUrlType urlType = DATABASE;
	private String tnsFolder;
	private String tnsProfile;

    public DatabaseInfo() {}

    public DatabaseInfo(String vendor, String host, String port, String database, DatabaseUrlType urlType) {
        this.vendor = vendor;
        this.host = host;
        this.port = port;
        this.database = database;
        this.urlType = urlType;
    }

    public DatabaseInfo(String vendor, String file, DatabaseUrlType urlType) {
        this.vendor = vendor;
        this.fileBundle = new DatabaseFileBundle(file);
        this.urlType = urlType;
    }

    public boolean isEmpty() {
        return Strings.isEmpty(host) &&
                Strings.isEmpty(port) &&
                Strings.isEmpty(database) &&
                Strings.isEmpty(tnsFolder) &&
                Strings.isEmpty(tnsProfile) &&
                Strings.isEmpty(getFirstFilePath());
    }

    public String ensureTnsFolder() {
        return Strings.isEmptyOrSpaces(tnsFolder) ? TnsAdmin.location() : tnsFolder;
    }

    public void reset() {
        this.host = null;
        this.port = null;
        this.database = null;
        this.tnsFolder = null;
        this.tnsProfile = null;
        this.fileBundle = null;
        this.url = null;
    }

    public void initializeUrl(DatabaseUrlPattern urlPattern) {
        this.url = urlPattern.buildUrl(this);
    }

    public void initializeDetails(DatabaseUrlPattern pattern) {
        if (Strings.isEmptyOrSpaces(url)) return;

        this.vendor = pattern.getDefaultInfo().getVendor();
        this.host = pattern.resolveHost(url);
        this.port = pattern.resolvePort(url);
        this.database = pattern.resolveDatabase(url);
        this.tnsFolder = pattern.resolveTnsProfile(url);
        this.tnsProfile = pattern.resolveTnsFolder(url);
        initializeFiles(pattern);
    }

    private void initializeFiles(DatabaseUrlPattern pattern) {
        String filePath = pattern.resolveFile(url);
        if (Strings.isNotEmptyOrSpaces(filePath)) {
            DatabaseFileBundle fileBundle = ensureFileBundle();
            DatabaseFile mainFile = fileBundle.getMainFile();
            if (mainFile == null) {
                fileBundle.add(new DatabaseFile(filePath, "main"));
            } else {
                mainFile.setPath(filePath);
            }
        }
    }


    @Nullable
    public DatabaseFileBundle getFileBundle() {
        return fileBundle;
    }

    @NotNull
    public DatabaseFileBundle ensureFileBundle() {
        if (fileBundle == null) fileBundle = new DatabaseFileBundle();
        return fileBundle;
    }

    public String getMainFilePath() {
        return fileBundle == null ? null : fileBundle.getMainFilePath();
    }

    public String getFirstFilePath() {
        return fileBundle == null ? null : fileBundle.getFirstFilePath();
    }

    public List<DatabaseFile> getAttachedFiles() {
        return fileBundle == null ? Collections.emptyList() : fileBundle.getAttachedFiles();
    }


    public boolean isCustomUrl() {
        return getUrlType() == CUSTOM;
    }

    @Override
    public DatabaseInfo clone() {
        DatabaseInfo clone = new DatabaseInfo();
        clone.vendor = this.vendor;
        clone.host = this.host;
        clone.port = this.port;
        clone.database = this.database;
        clone.url = this.url;
        clone.fileBundle = this.fileBundle == null ? null : this.fileBundle.clone();
        clone.urlType = this.urlType;
        clone.tnsFolder = this.tnsFolder;
        clone.tnsProfile = this.tnsProfile;

        return clone;
    }

}
