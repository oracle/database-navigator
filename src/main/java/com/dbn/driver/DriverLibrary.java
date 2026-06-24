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

package com.dbn.driver;

import lombok.Getter;
import lombok.SneakyThrows;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static com.dbn.driver.DriverLibraryScanner.validateClassEntryCount;
import static com.dbn.driver.DriverLibraryScanner.validateClassName;
import static com.dbn.driver.DriverLibraryScanner.validateEntryCount;
import static com.dbn.driver.DriverLibraryScanner.validateJarSize;
import static com.dbn.driver.DriverLibraryScanner.validateScanTime;
import static com.intellij.openapi.progress.ProgressManager.checkCanceled;

@Getter
public class DriverLibrary {
    private final File jar;
    private final Set<String> classNames = new LinkedHashSet<>();

    @SneakyThrows
    public DriverLibrary(File jar) {
        this.jar = jar;
        validateJarSize(jar);

        try (JarFile jarFile = new JarFile(jar)) {
            long startedAt = System.currentTimeMillis();
            int entryCount = 0;
            int classEntryCount = 0;
            int totalClassNameBytes = 0;

            var entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                checkCanceled();
                validateScanTime(startedAt);
                validateEntryCount(++entryCount);

                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;

                String name = entry.getName();
                if (!name.endsWith(".class")) continue;

                validateClassEntryCount(++classEntryCount);

                String className = name.replace('/', '.').substring(0, name.length() - 6);
                totalClassNameBytes += className.getBytes(StandardCharsets.UTF_8).length;
                validateClassName(className, totalClassNameBytes);
                this.classNames.add(className);
            }
        }
    }
}
