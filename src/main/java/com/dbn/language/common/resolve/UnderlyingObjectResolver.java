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

package com.dbn.language.common.resolve;

import com.dbn.language.common.psi.IdentifierPsiElement;
import com.dbn.object.common.DBObject;
import org.jetbrains.annotations.NonNls;

import java.util.HashMap;
import java.util.Map;

public abstract class UnderlyingObjectResolver {
    public static Map<String, UnderlyingObjectResolver> RESOLVERS = new HashMap<>();
    static {
        // TODO remove this and register as app component in plugin xml
        AliasObjectResolver.getInstance();
        AssignmentObjectResolver.getInstance();
        LocalDeclarationObjectResolver.getInstance();
        SurroundingVirtualObjectResolver.getInstance();
    }

    private final String id;

    public UnderlyingObjectResolver(@NonNls String id) {
        this.id = id;
        RESOLVERS.put(id, this);

    }

    public static UnderlyingObjectResolver get(String id) {
        return RESOLVERS.get(id);
    }

    public final DBObject resolve(IdentifierPsiElement identifierPsiElement) {
        return resolve(identifierPsiElement, 0);
    }

    protected abstract DBObject resolve(IdentifierPsiElement identifierPsiElement, int recursionCheck);
}
