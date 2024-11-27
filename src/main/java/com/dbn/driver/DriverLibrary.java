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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

@Getter
public class DriverLibrary {
    private final File jar;
    private final Set<String> classNames = new LinkedHashSet<>();

    @SneakyThrows
    public DriverLibrary(File jar) {
        this.jar = jar;
        try (JarFile jarFile = new JarFile(jar)) {
            List<String> classNames = Collections.list(jarFile.entries())
                    .stream()
                    .map(e -> e.getName())
                    .filter(n -> n.endsWith(".class"))
                    .map(n -> n.replaceAll("/", "."))
                    .map(n -> n.substring(0, n.length() - 6))
                    .map(n -> n.intern())
                    .collect(Collectors.toList());

            this.classNames.addAll(classNames);
        }
    }
}
