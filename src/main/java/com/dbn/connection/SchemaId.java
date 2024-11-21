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

import com.dbn.common.constant.PseudoConstant;
import com.dbn.common.constant.PseudoConstantConverter;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.Presentable;
import com.dbn.object.DBSchema;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public final class SchemaId extends PseudoConstant<SchemaId> implements Presentable {
    public static final SchemaId NONE = get("NONE");
    public static final SchemaId NULL = get("NULL");

    public SchemaId(String id) {
        super(id);
    }

    public static SchemaId get(String id) {
        return PseudoConstant.get(SchemaId.class, id);
    }

    @NotNull
    @Override
    public String getName() {
        return id();
    }

    public static SchemaId from(DBSchema schema) {
        return schema == null ? null : schema.getIdentifier();
    }

    @Override
    public Icon getIcon() {
        return Icons.DBO_SCHEMA;
    }

    public static class Converter extends PseudoConstantConverter<SchemaId> {
        public Converter() {
            super(SchemaId.class);
        }
    }

    public boolean is(String id){
        return id().equalsIgnoreCase(id);
    }
}
