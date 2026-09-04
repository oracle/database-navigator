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

package com.dbn.object.factory.model;

import com.dbn.common.constant.PseudoConstant;
import com.dbn.database.DatabaseIdentifierCase;
import com.dbn.object.DBCredential;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBCredentialType;
import com.dbn.object.type.DBJavaClassType;
import com.dbn.object.type.DBMiningModelSourceType;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.util.Unsafe.cast;


@Getter
@SuppressWarnings("unused")
public class DBObjectAttributeType<T> extends PseudoConstant<DBObjectAttributeType<T>> {

    public static final DBObjectAttributeType<DBObjectType> OBJECT_TYPE = new DBObjectAttributeType<>("OBJECT_TYPE", DBObjectType.class);
    public static final DBObjectAttributeType<String> OWNER_NAME = new DBObjectAttributeType<>("OWNER_NAME", String.class);
    public static final DBObjectAttributeType<String> OBJECT_NAME = new DBObjectAttributeType<>("OBJECT_NAME", String.class);
    public static final DBObjectAttributeType<String> OBJECT_DETAIL = new DBObjectAttributeType<>("OBJECT_DETAIL", String.class);
    public static final DBObjectAttributeType<String> DATA_TYPE = new DBObjectAttributeType<>("DATA_TYPE", String.class);

    public static final DBObjectAttributeType<Integer> DATA_LENGTH = new DBObjectAttributeType<>("DATA_LENGTH", Integer.class);
    public static final DBObjectAttributeType<Integer> DATA_PRECISION = new DBObjectAttributeType<>("DATA_PRECISION", Integer.class);

    public static final DBObjectAttributeType<Boolean> IS_INPUT = new DBObjectAttributeType<>("IS_INPUT", Boolean.class);
    public static final DBObjectAttributeType<Boolean> IS_OUTPUT = new DBObjectAttributeType<>("IS_OUTPUT", Boolean.class);
    public static final DBObjectAttributeType<Boolean> IS_NOT_NULL = new DBObjectAttributeType<>("IS_NOT_NULL", Boolean.class);
    public static final DBObjectAttributeType<Boolean> IS_PRIMARY_KEY = new DBObjectAttributeType<>("IS_PRIMARY_KEY", Boolean.class);

    public static final DBObjectAttributeType<String> CONSTRAINT_TYPE = new DBObjectAttributeType<>("CONSTRAINT_TYPE", String.class);
    public static final DBObjectAttributeType<String[]> CONSTRAINT_COLUMNS = new DBObjectAttributeType<>("CONSTRAINT_COLUMNS", String[].class);

    public static final DBObjectAttributeType<String> INDEX_DEFINITION = new DBObjectAttributeType<>("INDEX_DEFINITION", String.class);
    public static final DBObjectAttributeType<String[]> INDEX_COLUMNS = new DBObjectAttributeType<>("INDEX_COLUMNS", String[].class);
    public static final DBObjectAttributeType<DBObjectSpec> RETURN_ARGUMENT = new DBObjectAttributeType<>("RETURN_ARGUMENT", DBObjectSpec.class);

    public static final DBObjectAttributeType<DBMiningModelSourceType> MINING_MODEL_SOURCE_TYPE = new DBObjectAttributeType<>("MINING_MODEL_SOURCE_TYPE", DBMiningModelSourceType.class);
    public static final DBObjectAttributeType<String> MINING_MODEL_SOURCE_LOCATION = new DBObjectAttributeType<>("MINING_MODEL_SOURCE_LOCATION", String.class);
    public static final DBObjectAttributeType<DBObjectRef<DBCredential>> MINING_MODEL_CREDENTIAL = new DBObjectAttributeType<>("MINING_MODEL_CREDENTIAL");

    public static final DBObjectAttributeType<DBJavaClassType> JAVA_CLASS_TYPE = new DBObjectAttributeType<>("JAVA_CLASS_TYPE", DBJavaClassType.class);
    public static final DBObjectAttributeType<String> JAVA_PACKAGE_NAME = new DBObjectAttributeType<>("JAVA_PACKAGE_NAME", String.class);
    public static final DBObjectAttributeType<String> JAVA_CLASS_NAME = new DBObjectAttributeType<>("JAVA_CLASS_NAME", String.class);

    public static final DBObjectAttributeType<DBCredentialType> CREDENTIAL_TYPE = new DBObjectAttributeType<>("CREDENTIAL_TYPE", DBCredentialType.class);
    public static final DBObjectAttributeType<String> USER_NAME = new DBObjectAttributeType<>("USER_NAME", String.class);
    public static final DBObjectAttributeType<String> USER_OCID = new DBObjectAttributeType<>("USER_OCID", String.class);
    public static final DBObjectAttributeType<String> TENANCY_OCID = new DBObjectAttributeType<>("TENANCY_OCID", String.class);
    public static final DBObjectAttributeType<String> PRIVATE_KEY = new DBObjectAttributeType<>("PRIVATE_KEY", String.class);
    public static final DBObjectAttributeType<String> FINGERPRINT = new DBObjectAttributeType<>("FINGERPRINT", String.class);
    public static final DBObjectAttributeType<char[]> PASSWORD = new DBObjectAttributeType<>("PASSWORD", char[].class);
    public static final DBObjectAttributeType<char[]> ACCESS_TOKEN = new DBObjectAttributeType<>("ACCESS_TOKEN", char[].class);
    public static final DBObjectAttributeType<DatabaseIdentifierCase> IDENTIFIER_CASE = new DBObjectAttributeType<>("IDENTIFIER_CASE", DatabaseIdentifierCase.class);

    private final Class<T> type;

    @Nullable
    public T of(@Nullable DBObjectSpec spec) {
        if (spec == null) return null;
        return spec.getAttributeValue(this);
    }

    public boolean is(DBObjectSpec spec) {
        if (type != Boolean.class) throw new IllegalArgumentException("Only supported for boolean attributes");
        return spec.getBooleanAttributeValue(cast(this));
    }

    public static <T> DBObjectAttributeType<T> get(String id) {
        return PseudoConstant.get(DBObjectAttributeType.class, id);
    }

    private DBObjectAttributeType(@NonNls String id) {
        super(id);
        this.type = cast(Object.class);
    }

    private DBObjectAttributeType(@NonNls String id, Class<T> type) {
        super(id);
        this.type = type;
    }
}
