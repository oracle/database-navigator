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

package com.dbn.generator;

import com.dbn.common.util.Naming;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AliasBundle {
    private final Map<DBObjectRef, String> aliases = new HashMap<>();

    public String getAlias(DBObject object) {
        DBObjectRef objectRef = object.ref();
        String alias = aliases.get(objectRef);
        if (alias == null) {
            alias = Naming.createAliasName(object.getName());
            alias = getNextAvailable(alias);
            aliases.put(objectRef, alias);
        }
        return alias;
    }

    private String getNextAvailable(String alias) {
        for (String availableAlias : aliases.values()) {
            if (Objects.equals(alias, availableAlias)) {
                alias = Naming.nextNumberedIdentifier(alias, false);
            }
        }
        return alias;
    }

}
