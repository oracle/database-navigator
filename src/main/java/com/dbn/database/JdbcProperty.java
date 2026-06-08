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

package com.dbn.database;

import com.dbn.common.property.Property;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.context.DatabaseContext;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.nls.NlsResources.txt;

@Getter
public enum JdbcProperty implements Property.IntBase {
    MD_CATALOGS(txt("cfg.connection.const.JdbcProperty_MD_CATALOGS"), true),
    MD_SCHEMAS(txt("cfg.connection.const.JdbcProperty_MD_SCHEMAS"), true),
    MD_TABLES(txt("cfg.connection.const.JdbcProperty_MD_TABLES"), true),
    MD_VIEWS(txt("cfg.connection.const.JdbcProperty_MD_VIEWS"), true),
    MD_COLUMNS(txt("cfg.connection.const.JdbcProperty_MD_COLUMNS"), true),
    MD_PSEUDO_COLUMNS(txt("cfg.connection.const.JdbcProperty_MD_PSEUDO_COLUMNS"), true),
    MD_INDEXES(txt("cfg.connection.const.JdbcProperty_MD_INDEXES"), true),
    MD_PRIMARY_KEYS(txt("cfg.connection.const.JdbcProperty_MD_PRIMARY_KEYS"), true),
    MD_IMPORTED_KEYS(txt("cfg.connection.const.JdbcProperty_MD_IMPORTED_KEYS"), true),
    MD_FUNCTIONS(txt("cfg.connection.const.JdbcProperty_MD_FUNCTIONS"), true),
    MD_FUNCTION_COLUMNS(txt("cfg.connection.const.JdbcProperty_MD_FUNCTION_COLUMNS"), true),
    MD_PROCEDURES(txt("cfg.connection.const.JdbcProperty_MD_PROCEDURES"), true),
    MD_PROCEDURE_COLUMNS(txt("cfg.connection.const.JdbcProperty_MD_PROCEDURE_COLUMNS"), true),
    SQL_DATASET_ALIASING(txt("cfg.connection.const.JdbcProperty_SQL_DATASET_ALIASING"), true),

    CATALOG_AS_OWNER(txt("cfg.connection.const.JdbcProperty_CATALOG_AS_OWNER"), false),
    ;

    public static final JdbcProperty[] VALUES = values();

    private final String description;
    private final boolean feature;
    private final IntMasks masks = new IntMasks(this);

    JdbcProperty(String description, boolean feature) {
        this.description = description;
        this.feature = feature;
    }

    @Override
    public IntMasks masks() {
        return masks;
    }

    public boolean isSupported(@Nullable ConnectionHandler connection) {
        return isValid(connection) && connection.getCompatibility().is(this);
    }

    public boolean isSupported(@Nullable DatabaseContext connectionProvider) {
        return isValid(connectionProvider) && isSupported(connectionProvider.getConnection());
    }
}
