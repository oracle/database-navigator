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

package com.dbn.common.file.util;

import com.dbn.common.util.Strings;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.Arrays;

public class FileSearchRequest {
    private final String[] names;
    private final String[] patterns;
    private final String[] extensions;

    private FileSearchRequest(String[] names, String[] patterns, String[] extensions) {
        this.names = names;
        this.patterns = patterns;
        this.extensions = extensions;
    }

    public static FileSearchRequest forNames(String ... names) {
        return new FileSearchRequest(names, null, null);
    }

    public static FileSearchRequest forPatterns(String ... patterns) {
        return new FileSearchRequest(null, patterns, null);
    }

    public static FileSearchRequest forExtensions(String ... extensions) {
        return new FileSearchRequest(null, null, extensions);
    }

    public boolean matches(VirtualFile file) {
        if (names != null) {
            return Arrays.stream(names).anyMatch(name -> Strings.equalsIgnoreCase(file.getName(), name));
        } else if (patterns != null) {
            return Arrays.stream(patterns).anyMatch(pattern -> file.getName().matches(pattern));
        } else if (extensions != null) {
            return Arrays.stream(extensions).anyMatch(extension -> Strings.equalsIgnoreCase(file.getExtension(), extension));
        }
        return false;
    }
}
