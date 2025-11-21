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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * This enum is for listing the possible ways of creating a new credential
 *
 * @author Ayoub Aarrasse (Oracle)
 */
public enum DBCredentialType implements Constant<DBCredentialType> {
    PASSWORD,
    TOKEN,
    OCI;

    public static final Set<DBCredentialType> VECTOR_AI_TYPES = linkedSet(TOKEN);
    public static final Set<DBCredentialType> SELECT_AI_TYPES = linkedSet(PASSWORD, OCI);
    public static final Set<DBCredentialType> ALL_TYPES = linkedSet(PASSWORD, TOKEN, OCI);

    public static Set<DBCredentialType> getVectorAITypes() {
        return VECTOR_AI_TYPES;
    }

    public static Set<DBCredentialType> getSelectAITypes() {
        return SELECT_AI_TYPES;
    }

    public static Set<DBCredentialType> getAllTypes() {
        return ALL_TYPES;
    }

    private static Set<DBCredentialType> linkedSet(DBCredentialType ... types) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(List.of(types)));
    }
}
