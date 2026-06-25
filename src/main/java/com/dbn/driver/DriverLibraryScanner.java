/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.driver;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static com.dbn.nls.NlsResources.txt;

final class DriverLibraryScanner {
    static final int MAX_JAR_COUNT = 64;
    static final int MAX_JAR_ENTRIES = 50_000;
    static final int MAX_CLASS_ENTRIES = 20_000;
    static final int MAX_CLASS_NAME_LENGTH = 512;
    static final int MAX_TOTAL_CLASS_NAME_BYTES = 4 * 1024 * 1024;
    static final long MAX_JAR_SIZE_BYTES = 256L * 1024 * 1024;
    static final long MAX_SCAN_MILLIS = 15_000L;

    private DriverLibraryScanner() {}

    static List<File> getLoadableJars(File library) throws IOException {
        List<File> jars = collectLoadableJars(library);
        if (jars.size() > MAX_JAR_COUNT) {
            throw invalidDriverLibrary();
        }

        for (File jar : jars) {
            validateJarSize(jar);
        }
        return jars;
    }

    static void validateJarSize(File jar) {
        if (jar.length() > MAX_JAR_SIZE_BYTES) {
            throw invalidDriverLibrary();
        }
    }

    static void validateScanTime(long startedAt) {
        if (System.currentTimeMillis() - startedAt > MAX_SCAN_MILLIS) {
            throw invalidDriverLibrary();
        }
    }

    static void validateEntryCount(int entryCount) {
        if (entryCount > MAX_JAR_ENTRIES) {
            throw invalidDriverLibrary();
        }
    }

    static void validateClassEntryCount(int classEntryCount) {
        if (classEntryCount > MAX_CLASS_ENTRIES) {
            throw invalidDriverLibrary();
        }
    }

    static void validateClassName(String className, int totalClassNameBytes) {
        if (className.length() > MAX_CLASS_NAME_LENGTH || totalClassNameBytes > MAX_TOTAL_CLASS_NAME_BYTES) {
            throw invalidDriverLibrary();
        }
    }

    private static List<File> collectLoadableJars(File library) throws IOException {
        File canonicalLibrary = library.getCanonicalFile();
        if (!canonicalLibrary.isDirectory()) return List.of(canonicalLibrary);

        File[] jars = canonicalLibrary.listFiles(file -> file.isFile() && file.getName().endsWith(".jar"));
        if (jars == null) return List.of();

        return Arrays.stream(jars)
                .sorted(Comparator.comparing(File::getName))
                .toList();
    }

    private static IllegalArgumentException invalidDriverLibrary() {
        return new IllegalArgumentException(txt("cfg.connection.error.InvalidDriverLibrary"));
    }
}
