/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.object.factory.model.generic;

import com.dbn.common.constant.PseudoConstant;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;

import static com.dbn.common.util.Unsafe.cast;


@Getter
@SuppressWarnings("unused")
public class DBObjectAttribute<T> extends PseudoConstant<DBObjectAttribute<T>> {

    public static final DBObjectAttribute<String> OWNER_NAME = new DBObjectAttribute<>("OWNER_NAME", String.class);
    public static final DBObjectAttribute<String> OBJECT_DETAIL = new DBObjectAttribute<>("OBJECT_DETAIL", String.class);
    public static final DBObjectAttribute<String> DATA_TYPE = new DBObjectAttribute<>("DATA_TYPE", String.class);

    public static final DBObjectAttribute<Integer> DATA_LENGTH = new DBObjectAttribute<>("DATA_LENGTH", Integer.class);
    public static final DBObjectAttribute<Integer> DATA_PRECISION = new DBObjectAttribute<>("DATA_PRECISION", Integer.class);

    public static final DBObjectAttribute<Boolean> IS_INPUT = new DBObjectAttribute<>("IS_INPUT", Boolean.class);
    public static final DBObjectAttribute<Boolean> IS_OUTPUT = new DBObjectAttribute<>("IS_OUTPUT", Boolean.class);
    public static final DBObjectAttribute<Boolean> IS_NOT_NULL = new DBObjectAttribute<>("IS_NOT_NULL", Boolean.class);
    public static final DBObjectAttribute<Boolean> IS_PRIMARY_KEY = new DBObjectAttribute<>("IS_PRIMARY_KEY", Boolean.class);

    public static final DBObjectAttribute<String> CONSTRAINT_TYPE = new DBObjectAttribute<>("CONSTRAINT_TYPE", String.class);
    public static final DBObjectAttribute<String[]> CONSTRAINT_COLUMNS = new DBObjectAttribute<>("CONSTRAINT_COLUMNS", String[].class);

    private final Class<T> type;

    public static <T> DBObjectAttribute<T> get(String id) {
        return PseudoConstant.get(DBObjectAttribute.class, id);
    }

    private DBObjectAttribute(@NonNls String id) {
        super(id);
        this.type = cast(Object.class);
    }

    private DBObjectAttribute(@NonNls String id, Class<T> type) {
        super(id);
        this.type = type;
    }
}
