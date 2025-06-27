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

package com.dbn.debugger.jdwp;

import com.dbn.common.util.Strings;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Getter
@EqualsAndHashCode
public class DBJdwpSourcePath {
    private final String signature;
    private final String programType;
    private final String programOwner;
    private final String programName;

    private DBJdwpSourcePath(String sourceUrl) {
        if (sourceUrl.startsWith("$Oracle")) {
            String[] tokens;
            if (sourceUrl.contains("\\")) {
                tokens = sourceUrl.split("[\\\\.:]");
            } else if (sourceUrl.contains("/")) {
                tokens = sourceUrl.split("[/.:]");
            } else {
                tokens = sourceUrl.split("[.:]");
            }

            if (tokens.length < 4) {
                throw new UnsupportedOperationException("Cannot tokenize source path: " + sourceUrl);
            }
            signature = tokens[0];
            programType = tokens[1];
            programOwner = tokens[2];
            programName = tokens[3];
        } else {
            signature = null;
            programType = "JavaClass";
            programName = sourceUrl;
            programOwner = null;
        }
    }

    public boolean isAnonymousBlock() {
        return Objects.equals(programType, "Block");
    }

    public boolean isJavaProgram() {
        return Objects.equals(programType, "JavaClass");
    }

    public boolean isDatabaseProgram() {
        return !isAnonymousBlock() && !isJavaProgram();
    }

    public boolean isProgramBody() {
        return Strings.isOneOf(programType, "PackageBody", "TypeBody");
    }

    public static DBJdwpSourcePath from(@NotNull String sourceUrl) throws Exception {
        return new DBJdwpSourcePath(sourceUrl);
    }

    @Override
    public String toString() {
        return isJavaProgram() ? "[JAVA_CLASS] " + programName :
                isDatabaseProgram() ? "[DB_PROGRAM] " + programOwner + "." + programName :
                isAnonymousBlock() ? "[ANONYMOUS_BLOCK] " : "Unknown";
    }
}
