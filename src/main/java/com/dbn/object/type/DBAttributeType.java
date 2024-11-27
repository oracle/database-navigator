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

package com.dbn.object.type;

import com.dbn.common.constant.Constant;
import lombok.Getter;

@Getter
public enum DBAttributeType implements Constant<DBAttributeType> {
    // Credential attributes
    USER_NAME("username"),
    PASSWORD("password"),
    USER_OCID("user_ocid"),
    USER_TENANCY_OCID("user_tenancy_oci"),
    PRIVATE_KEY("private_key"),
    FINGERPRINT("fingerprint"),

    // Profile attributes
    MODEL("model"),
    PROVIDER("provider"),
    TEMPERATURE("temperature"),
    OBJECT_LIST("object_list"),
    CREDENTIAL_NAME("credential_name"),

    ;

    private final String id;

    DBAttributeType(String id) {
        this.id = id;
    }
}
