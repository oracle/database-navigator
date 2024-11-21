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

package com.dbn.language.common.element.parser;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Objects;

@Getter
@EqualsAndHashCode(callSuper = true)
public class BranchCheck extends Branch{
    private double version = 0;
    private Type type;

    public boolean check(Branch branch, double currentVersion) {
        switch (type) {
            case ALLOWED: return Objects.equals(name, branch.name) && currentVersion >= version;
            case FORBIDDEN: return !Objects.equals(name, branch.name) || currentVersion < version;
        }
        return true;
    }

    @Override
    public String toString() {
        return getName() + "@" + version;
    }

    public enum Type {
        ALLOWED,
        FORBIDDEN
    }

    public BranchCheck(String def) {
        int startIndex = 0;
        if (def.startsWith("-")) {
            type = Type.FORBIDDEN;
            startIndex = 1;
        } else if (def.startsWith("+")) {
            type = Type.ALLOWED;
            startIndex = 1;
        }

        int atIndex = def.indexOf("@", startIndex);
        if (atIndex > -1) {
            name = def.substring(startIndex, atIndex).trim();
            version = Double.parseDouble(def.substring(atIndex + 1));
        } else {
            name = def.substring(startIndex).trim();
        }
    }
}
