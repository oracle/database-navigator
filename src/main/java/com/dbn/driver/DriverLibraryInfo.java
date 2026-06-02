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

import lombok.Getter;
import lombok.SneakyThrows;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Getter
public class DriverLibraryInfo {
    private final File library;
    private final String path;
    private final boolean directory;
    private final List<File> jars;

    @SneakyThrows
    public DriverLibraryInfo(File library) {
        this.library = library.getCanonicalFile();
        this.path = this.library.getAbsolutePath();
        this.directory = this.library.isDirectory();

        this.jars = getLoadableJars(this.library);
    }

    private static List<File> getLoadableJars(File library) {
        if (!library.isDirectory()) return List.of(library);

        File[] jars = library.listFiles(file -> file.isFile() && file.getName().endsWith(".jar"));
        if (jars == null) return List.of();

        return Arrays.stream(jars)
                .sorted(Comparator.comparing(File::getName))
                .toList();
    }

    public int getJarCount() {
        return this.jars.size();
    }
}
